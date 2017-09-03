# ESPGame
ESP Game for the interview in SquadRun.

The repo has 2 main folders and a file

KushankESPGame: It is the folder having all the code for the android Application

SquadRunFunctions: It is the folder having cloud functions for the ESPGame.

esp-game-squadrun-export.json: It is the default database structure for Firebase Realtime Database.


The whole project works using following technologies:

1. Firebase Realtime Database.

2. Firebase Authentication.

3. Firebase Notification.

4. Firebase Cloud Storage.

5. Firebase Cloud Functions.

Work Flow: 
1. User logs in, after creating account, if required.
2. User clicks on the floating action bar, to create a new task.
3. The Android app will create a new node in database, saying "justCreated: true"
4. On seeing such a node created, cloud functions will get activated to initialise that node.
5. Questions are selected for the user.
5.1 Ques Ids are inserted in front of the user, along with the status/score for that question.
5.2 User's id and its Task's id is added in front of the questions node, where all the detail of the question is saved.
5.3 counter for the contestant is maintained.
6. The user selects answer for a question.
6.1 Android app updates the answer for the question
6.2 It activates another cloud function to do some tasks.
6.2.1 If the result for the question can be declared, then it is declared.
6.2.2 If the list to maintain consensus is to be manipulated, it is done.
6.2.3 Of the admin is to be notified for the consenses, it is done.
7. If the user exists the task in between:
7.1. The task is deleted from the user node.
7.2. If reuired, contestants are removed from the question's node.
8. If the user submits the task, his/her score is maintainted, else discarded.

Assumptions:
1. If the number of unattempted questions left in the database is less than 5, then all the remaining questions are provided as a single task.
2. If the user exits in between, and has scored 1 point for any question, then that point is not discarded, as the solution for that question is attained, but the user won't get the score.
3. If a question is wrongly attempted, then it is again eligible for the evaluation in another task.
4. Number of secondary images for a question is dynamic.
5. The user can change his answer, till he submits the task completely.
6. A player can play n number of tasks, till the consensus for all is not attained and the user have left some questions unattempted.
7. Muliple players can play consequently which can lead to a situation in which each the points for the whole task not necessarily determined by a single user, i.e, there may be a situation, where, from the 5 questions user A have got, 3 question are solved by user B and 2 by user C. (B and C may also have such sitation to complete they quota of 5 questions).

If given plenty of time,
1. The UI requires to be improved.
2. Multiple login methods will be introduced for user's ease.
3. Proper mechanism for the user to see his answers.
4. Proper mechanism for downloading the images (Not all at once).
5. Database and Storage security rules are to be constrained.
6. Redundant codes or computationally high codes, if encountered, are to be improved.
7. Proper testing is to be required.
