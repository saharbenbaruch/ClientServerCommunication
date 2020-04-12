In this project I used protocol UDP for communication between client and server.
Server listening on port 3117.

Message structure :
32  chars of teamName
1 char -  type of message
40 chars -hash message
1 char original length of answer
1-256 chars start of original string
1-256 chars end of original string.

Types:
1- discovery message send by client to everyone
2- respond by servers who active and got the type 1 message.
3- request message. send by client to servers who send him type=2 message.
4- ack . contain the answer . send from server to client
5- negative ack . send by server to client and mean "I dont have answer"

client divide domains between servers so they could solve simultaneous .
server use SHA 1 algorithm .