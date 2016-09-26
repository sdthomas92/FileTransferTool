# File Transfer Tool

This is a Java program that allows a user (client) to send any file to another user (server) who are running the same program on different computers connected to the Internet, or directly via ethernet. The program sends files in packets that are encrypted using a simple XOR cipher, the key being any file type that both the client and server must have. The files are also hashed and compared on both ends to ensure there is no data loss or corruption. This was originally a project in a Networking class, and was a group project. Because of this, the program was designed to have a login feature that works only with specific login credentials.

## Features
* Sends any file from the client to the server (assuming both parties are running the same program and have the same key)
* Uses a key (XOR cipher encryption)
* The file is broken up in packets which are encypted
* The packets are hashed and the hash is attached to the packet, and the server hashes the file portion to compare with the attached hash.
