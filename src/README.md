# Source files

## PastebinAPI.kt

An object that contains all the necessary methods to access Pastebin using its API.

## Paste.kt

A class that encapsulates a Pastebin paste. Paste information is given by the API in XML format, which is parsed into this class. The user is advised to create a paste class and paste XML parser that suits their needs.

## PastebinContext

A class containing the devkey and userkey of a user. Facilitates the calling of API requests without having to supply this information every time.
