

# Time Manager

## Overview

 Time manager app DB, Backend (KTor) and Android App done in about 30h (in a 2 week timeframe) for last step of a hi-tech screening process.

## Implementation

 - Android Native (Kotlin) app
 - Ktor Backend w/ Exposed ORM
 - DB: MySql
 
 Please check specifics for frontend/backend to run the project in your local enviroment.

## Specifics

For more details please refer to the following links:

### Backend

 - [README](https://github.com/rodvar/timemanager/backend/README.md)
 - [Postman Collection to Test/Doc Backend](https://github.com/rodvar/timemanager/backend/docs/TimeManagerAPI.postman_collection.json)

### Frontend

 - [README](https://github.com/rodvar/timemanager/frontend/README.md)


### Project Initial Requirements

The requirements for the test project are:
Write an application for time management system

User must be able to create an account and log in. (If a mobile application, this means that more users can use the app from the same phone).
User can add (and edit and delete) a row describing what they have worked on, what date, and for how long.
User can add a setting (Preferred working hours per day).
If on a particular date a user has worked under the PreferredWorkingHourPerDay, these rows are red, otherwise green.
Implement at least three roles with different permission levels: a regular user would only be able to CRUD on their owned records, a user manager would be able to CRUD users, and an admin would be able to CRUD all records and users.
Filter entries by date from-to.
Export the filtered times to a sheet in HTML:
  * Date: 2018.05.21
  * Total time: 9h
  * Notes:
    * Note1
    * Note2
    * Note3
REST API. Make it possible to perform all user actions via the API, including authentication (If a mobile application and you don’t know how to create your own backend you can use Firebase.com or similar services to create the API).
In any case, you should be able to explain how a REST API works and demonstrate that by creating functional tests that use the REST Layer directly. Please be prepared to use REST clients like Postman, cURL, etc. for this purpose.
If it’s a web application, it must be a single-page application. All actions need to be done client-side using AJAX, refreshing the page is not acceptable. (If a mobile application, disregard this).
Functional UI/UX design is needed. You are not required to create a unique design, however, do follow best practices to make the project as functional as possible.
Bonus: unit and e2e tests.

Helpful take-home project guidelines:

• This project will be used to evaluate your skills and should be fully functional without any obvious missing pieces. We will evaluate the project as if you were delivering it to a customer.
• The deadline to submit your completed project is 2 weeks from the date you received the project requirements.
• If you schedule your final interview after the 2-week deadline, make sure to submit your completed project and all code to the private repository before the deadline. Everything that is submitted after the deadline will not be taken into consideration.
• Please do not commit any code at least 12 hours before the meeting time so that it can be reviewed. Anything that is submitted after this time will not be taken into consideration.
• Please join the meeting room for this final interview on time. If you miss your interview without providing any prior notice, your application may be paused for six months.

Please schedule an interview time that is most suitable for you. Bear in mind that you will need to show a finished project during this interview.

Once you pick an appointment time, we’ll email you with additional meeting details and the contact details of another senior developer from our team who will assess your project and conduct your next interview. They are acting as your client for this project and are your point of contact for any questions you may have during the development of this project.
