const functions = require('firebase-functions')
const admin = require('firebase-admin')
const CONSENSUS = 3
admin.initializeApp(functions.config().firebase)

//called when a new task is created
exports.userCreated = functions.database.ref('/users/{uid}/{taskid}')
    .onCreate(event => {

		console.log('userCreated Event Occured');

		//get the value of justCreated
    	var justCreated = event.data.child('justCreated').val();
    	
    	//if it is not justCreated, return
    	if(!justCreated)
    		return;

    	//Get the questions, it there are already a bunch of questions, exit, else create user.
    	var quesRef = event.data.ref.child('ques');
		return quesRef.once('value').then(snapshot=>{
			if(snapshot.exists())
				return;
			else
				return userCreation(event);
			}
		);
	});

//called when ans is changed or added.
exports.ansSelected = functions.database.ref('primImage/{img}/contestant/{uid}/ans')
	.onWrite(event => {
		console.log('ansSelected Event Occured');

		//count the number of contestants.
		var countRef = event.data.ref.parent.parent.parent.child('count');

		//call ansSelection for further manipulation
		return countRef.once('value').then(snapshot => {
			return ansSelection(event, (snapshot.val()<CONSENSUS));
		});
	});

//called when task is abruptly deleted
//i.e., when the user exits midway.
exports.taskDeleted = functions.database.ref('/users/{uid}/{taskid}')
	.onDelete(event => {
		console.log('taskDeleted Event Occured');

		//get the data before deletion.
		var prevData = event.data.previous; 
		var img = []
		var userName = event.data.ref.parent.key;
		prevData.child('ques').forEach(ch => {
			//if the ans for the ques is evaluated, and it is wrong, skip, because it is already deleted.
			if(ch.val()==-1)
				img.push(ch.key);
		});
		var promises = [];
		var rootRef = event.data.ref.root;
		// decrement count and delete contestant for each image
		for(var i=0; i<img.length; i++)
		{
			var imgRef = rootRef.child('primImage/'+img[i]+'/contestant/'+userName);
			promises.push( imgRef.remove() );

			var countRef = rootRef.child('primImage/' + img[i] + '/count');
			promises.push(
				countRef.transaction(current =>{
					return (current || 0) - 1;
				})
			);
		}
		

		var toBeDel = [];
		var toBeAdded = [];

		var unAnsRef = rootRef.child('unanswered');

		//update unanswered based upon the status of each question.
		//if a question is in unanswered, remove it, else add it in unanswered, unless it is answered.
		promises.push( unAnsRef.once('value').then(snapshot=>{
				var valDel = []
				
				return countRef.transaction(current =>{
					return (current || 0) + 1;
				});

				/*
				snapshot.forEach(q=>{
					if(q.key=='count') return;
					if(img.indexOf(q.val())!=-1){
						toBeDel.push(q.key);
						valDel.push(q.val());
					}
				});
				for(var i=0; i<img.length; i++)
					if(valDel.indexOf(img[i])==-1)
						toBeAdded.push(img[i]);
				var nestPromises = [];
				
				for(var i=0; i<toBeDel.length; i++)
					nestPromises.push( unAnsRef.child(toBeDel[i]).remove() );


				nestPromises.push( insertQuesUnanswered(toBeAdded, event.data.ref.root) );
				return Promise.all(nestPromises)
					.then(()=>{ return; })
					.catch(er => { console.error('An Error Occured: ', er); });		
				*/
			}
		));
		
		return Promise.all(promises)
				.then(()=>{ return; })
				.catch(er => { console.error('An Error Occured: ', er); });
	}
);

//for change in ans of any question by the user.
function ansSelection(event, notans){
	console.log('ansSelection Called');

	var rootRef = event.data.ref.root;
	var contestantRef = event.data.ref.parent.parent;
	return contestantRef.once('value').then(snapshot=>{
		var same = true;
		var ans = null;
		var user = [];
		var task = [];
	
		// check if all the answers to the given question are same or not.
		snapshot.forEach(ch=>{
			user.push(ch.key);
			task.push(ch.child('tid').val());

			if(ch.child('ans').val()==null)
			{
				ans = null;
				same = false;
			}else if(same == false)
				;
			else if(ans==null)
				ans = ch.child('ans').val();
			else if(ans != ch.child('ans').val())
				same = false;
			
		});

		var promises = [];

		// if the answer cannot be evaluated, then set the status to -1 for the question.
		if(ans == null || notans)
		{
			for(var i=0; i<user.length; i++)
				promises.push(
					changePoint(user[i], task[i], contestantRef.parent.key, -1, event.data.ref.root)
				);	
		}
		// if the answers are same, give 1 point to each user.
		else if(same)
		{
			for(var i=0; i<user.length; i++)
				promises.push(
					changePoint(user[i], task[i], contestantRef.parent.key, 1, event.data.ref.root)
				);
			promises.push(
				rootRef.child('unanswered').remove()
			)
		}
		// if the answers are not same, give 0 point to each user.
		// remove the user's as contestant for the question
		// set the count of contestants to 0.
		else{
			for(var i=0; i<user.length; i++)
				promises.push(
					changePoint(user[i], task[i], contestantRef.parent.key, 0, event.data.ref.root)
				);
			
			promises.push(
				rootRef.child('unanswered').remove()
			)

			//set count of contestant to 0
			promises.push(
				contestantRef.parent.child('count').set(0)
			);

			//remove all the contestants for that question, to get it again for evalution the next time.
			promises.push(
				contestantRef.remove()
			);
		}

		// if the consensus for all the questions have reached, then notify the user.
		promises.push( ifNotifyAdmin(event.data.ref.root) );

		return Promise.all(promises)
					.then(()=>{ return; })
					.catch(er => { console.error('An Error Occured: ', er); });
	});	
}

// if the consensus is reached, notify the user.
function ifNotifyAdmin(rootRef){
	return rootRef.child('primImage').once('value').then(snapshot=>{
			var reached = true;

			// if consensus is reached for each question.
			snapshot.forEach(ques => {

				// if the count of contestant for the question is not 2, then set reached to false, and break the forEach loop by returing true.
				if(ques.child('count').val()<CONSENSUS){reached = false; return true;}

				// if the contestants are 2, then check if all the answers are same.
				var ans = null;
				ques.child('contestant').forEach(user => {
					if(user.child('ans').val()==null)
					{
						reached = false;
						return true;
					}else if(ans==null)
						ans = user.child('ans').val();
					else if(ans != user.child('ans').val())
					{
						reached = false;
						return true;
					}
				})

				// if the consensus for this question is not reached, then break the loop.
				if(reached == false)
					return true;
			})

			// if consensus is reached, send the notification to the admin.
			if(reached)
				sendNoti();
			else
				console.log('Not reached consensus');
		});
}

//takes the number of point to be alloted to the user for a specific question of a specific task.
function changePoint(user, task, img, points, rootRef)
{
	var ref = rootRef.child('users/'+user+'/'+task+'/ques/'+img);
	return ref.set(points);
}

//called when the prerequisite matches for the creationf of user, i.e., assigning of questions for his task.
function userCreation(event){


	//var count=15;
	//get the userId of the user.
	var userName = event.data.ref.parent.key;

	//get the root reference.
	const rootRef = event.data.ref.root;
	
	console.log('userCreation called');

	//get the questions(where unanswered or new questions, and save them in quesIds for further manipulation)
	return getQuestions(userName, rootRef, quesIds=>{
		
		var quesRef = event.data.ref.child('ques');
		var value={};
		var promises=[];
		for(var i=0; i<quesIds.length; i++)
		{
			//Set question for user and its points as undetermined
			value[quesIds[i]]=-1;

			//Set userId, taskId for the question(image)
			var contestantRef = rootRef.child('primImage/' + quesIds[i]+'/contestant');
			promises.push(
				contestantRef.
				child(userName+'/tid').
				set(event.data.ref.key)
			);

			//update the count for the number of users for the current question
			var countRef = rootRef.child('primImage/' + quesIds[i] + '/count');
			promises.push(
				countRef.transaction(current =>{
					return (current || 0) + 1;
				})
			);
		}

		//set the questions for the user.
		promises.push(quesRef.update(value));

		//set justCreated as false
		promises.push(event.data.ref.child('justCreated').set(false));

		return Promise.all(promises).then(() => {
					return;
			}).catch(er => {
					console.error('An Error Occured: ', er);
			});
	});
}

//called to get get questions, irrespective of internal complexities.
function getQuestions(uName, rootRef, callback)
{
	var userRef = rootRef.child('users/'+uName);
	return userRef.once('value').then(tasks => {
		userQues = [];
		tasks.forEach(task => {
			ques = [];
			task.child('ques').forEach(que => {
				// if the question is not wrongly answered, then push it.
				if(que.val()!=0)
					ques.push(que.key);
			});

			//cumulate all the questions.
			userQues = userQues.concat(ques);
		});


		var unAnsRef = rootRef.child('unanswered');
		return unAnsRef.once('value').then(snapshot=>{
			if(snapshot.exists())
				//if unanswered questions exist.
				return getUnanswered(5, unAnsRef, snapshot, userQues, uName, rootRef, callback);
			else
				//if no unanswered questions exist.
				return getNewQuestions(5, uName, rootRef, userQues, callback);
		});
	});
}

//if unanswered questions exist, try to give the user the question he has not attempted,
// else get nw questions for the remaining lot.
function getUnanswered(nQues, ref, snapshot, userQues, uName, rootRef, callback)
{
	const ans=[]
	const keys = []

	// set the count to the min unanswered and the question required to be provided to the new user.
	var childCount = Math.min(snapshot.numChildren(), nQues);
	var unansCount = snapshot.child('count').val();
	snapshot.forEach(ch=>{
		if(userQues.indexOf(ch.val())==-1)
		{
			if(ch.key == 'count')
				return;
			ans.push(ch.val());
			keys.push(ch.key);
			childCount--;
			if(childCount==0)
				return 1;	
		}
	})

	var promises = [];

	/*
	if(unansCount==){
		// remove those questions from unanswered.
		for(var i=0; i<keys.length; i++)
			promises.push(ref.child(keys[i]).remove());
	}else
	*/
	{
		promises.push(
			ref.child('count').transaction(current =>{
				return (current || 0) - 1;
			})
		);
	}

	
	// send the questions to the user.
	promises.push(callback(ans));

	// if more questions are to be provided, then get new random questions.
	if(ans.length < nQues)
	{
		//already attempted and currently choosed answered questions should not be choosed again.
		var newQues = ans.concat(userQues);

		// get new questions.
		promises.push( getNewQuestions(nQues - ans.length, uName, rootRef, newQues, callback) ) ;
	}

	return Promise.all(promises).then(() => {
  		return;
	}).catch(er => {
  		console.error('An Error Occured: ', er);
	});
}

//get new questions.
function getNewQuestions(nQues, uName, rootRef, userQues, callback)
{
	var img = [];
	var imgRef = rootRef.child('primImage');

	// iterate for all the questions, which have less than 2 contestants and the user has not attempted it(unless he has wrongly attempted it).
	return imgRef.once('value').then(snapshot=>{
		var childCount = snapshot.numChildren();
		var promise=null;
		snapshot.forEach(ch=>{
			childCount--;
			if(ch.child('count').val()<CONSENSUS && userQues.indexOf(ch.key)==-1)
				img.push(ch.key);

			// if got all the possible questions, then call the util to get random questions.
			if(childCount==0)		
				promise = getQ_util(nQues, uName, img, rootRef, callback);
		})
		return promise;
	})
}

//get random questions, from the total available questions(img)
function getQ_util(nQues, uName, img, rootRef, callback)
{
	var nTotal = img.length;
	var ans = [];
	nQues = Math.min(nQues, nTotal);
	for(var i=0; i<nQues; i++)
	{
		var rand = Math.trunc(Math.random()*(nTotal-i));
		ans.push(img[rand]);
		img.splice(rand, 1);
	}

	var promises = [];
	
	//insert the questions in unanswered list, to get earlier consensus.
	promises.push(insertQuesUnanswered(ans, rootRef));

	//send the questions to the user.
	promises.push(callback(ans));

	return Promise.all(promises).then(() => {
  		return;
	}).catch(er => {
  		console.error('An Error Occured: ', er);
	});
}


//send all the (ques) to the unanswered list, for earlier consensus.
function insertQuesUnanswered(ques, rootRef)
{
	var unAnsRef = rootRef.child('unanswered');
	var nQues = ques.length;
	var promises = [];
	for(var i=0; i<nQues; i++)
	{
		promises.push(
			unAnsRef.push().set(ques[i])
		);
	}

	promises.push(
		unAnsRef.child('count').set(CONSENSUS)
	);

	return Promise.all(promises).then(() => {
  		return;
	}).catch(er => {
  		console.error('An Error Occured: ', er);
	});
}

//send the notification to the admin.
function sendNoti(){

	// the app for the admin is registered under the "admin" topic.
    var topic = "admin"
    var payload = {
        notification: {
    		title: "ESP Game",
    		body: "Consensus Reached for all questions",
    		"sound": "default"
  		}
    }

    var options = {
  		priority: "high",
		timeToLive: 60 * 60 * 24 * 7
	}

    return admin.messaging().sendToTopic(topic, payload, options)
        .then(function (response) {
            console.log("Successfully sent message:", response);
        })
        .catch(function (error) {
            console.log("Error sending message:", error);
        })
}