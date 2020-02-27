package main

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import kotlin.streams.toList

/**
 * Contains various (static) methods that are used to communicate with the Pastebin server using its API.
 */
object Connections {



    /*
    CONSTANTS
     */



    private val CHARSET = StandardCharsets.UTF_8.toString()

    /**
     * Requests for user information and paste information go to this URL.
     */
    private const val POST_URL = "https://pastebin.com/api/api_post.php"

    /**
     * Login requests to obtain user keys go to this URL.
     */
    private const val LOGIN_URL = "https://pastebin.com/api/api_login.php"

    /**
     * Requests to get the raw contents of a user's private pastes go to this URL.
     */
    private const val PRIVATE_RAW_URL = "https://pastebin.com/api/api_raw.php"

    /**
     * This URL is queried when getting the raw contents of a user's public pastes. This does not require the user's
     * key and is not actually part of the Pastebin API. The paste's key is appended to the end of the url and the
     * raw code is taken from the resulting web page.
     */
    private const val PUBLIC_RAW_URL = "https://pastebin.com/raw/"



    /*
    POST FORMATTING
     */



    /**
     * Converts the given [parameters] into a POST format. This is the required format for requests to the Pastebin API.
     * It is formatted as so: "parameter0Name=parameter0&parameter1name=parameter1..."
     */
    private fun toPost(parameters: Map<String, String>) : String {
        var post = ""

        for(p in parameters) {
            with(p) {
                if(value != "null") post += "$key=$value&"
            }
        }

        //Remove the trailing &.
        return post.trimEnd('&')
    }



    /*
    CONNECTION CREATION
     */



    /**
     * Opens a connection with the given [URL]. A connection can only handle one request so it is not important to
     * cache the result of this method. Instead, this should be called for every request made to the server.
     */
    private fun createConnection(url: URL) : URLConnection {
        val c = url.openConnection()

        //Some necessary formatting of the connection.
        c.useCaches = false
        c.doOutput = true
        c.setRequestProperty("accept-charset", "UTF-8")

        return c
    }



    /**
     * Version of [createConnection] with the [url] parameter given in string form.
     */
    private fun createConnection(url: String) = createConnection(URL(url))



    /**
     * Version of [createConnection] for connections with the [POST_URL]
     */
    private fun createPostConnection() = createConnection(POST_URL)



    /**
     * Version of [createConnection] for connections with the [LOGIN_URL]
     */
    private fun createLoginConnection() = createConnection(LOGIN_URL)



    /**
     * Version of [createConnection] for connections with the [PRIVATE_RAW_URL]
     */
    private fun createPrivateRawConnection() = createConnection(PRIVATE_RAW_URL)



    /*
    CONNECTION IO
     */



    /**
     * Constructs an [OutputStreamWriter] from the given [connection]'s output stream and the default [CHARSET]
     */
    private fun getWriter(connection: URLConnection) =
        OutputStreamWriter(connection.getOutputStream(), CHARSET)



    /**
     * Constructs a [BufferedReader] from the given [connection]'s input stream and the default [CHARSET]
     */
    private fun getReader(connection: URLConnection) =
        BufferedReader(InputStreamReader(connection.getInputStream(), CHARSET))



    /*
    CONNECTION COMMUNICATION
     */



    /**
     * Sends a [line] of text to the given [connection]. A connection can only handle one request, so the [getWriter]
     * method is called each time that this method is called. No caching takes place.
     */
    private fun sendTo(connection: URLConnection, line: String) =
        getWriter(connection).use { it.write(line) }



    /**
     * Receives the response from the given [connection] following a request. This must be called after a request is
     * made using the [sendTo] method. The return type is a [List] of [String]s. For single-line responses, only the
     * first index of the list will be present. A connection can only handle one request, so the [getReader] method is
     * called each time that this method is called. No caching takes place. This is a blocking function that will wait
     * for the server's response.
     */
    private fun receiveFrom(connection: URLConnection) : List<String> =
        getReader(connection).use { return it.lines().toList() }



    /**
     * Sends a [line] of text to the given [connection] and returns the response. This method blocks until the server's
     * response to the request is received. If the result is empty of begins with "Bad API request", a
     * [PastebinAPIException] is thrown with the given reason for the error.
     */
    private fun query(connection: URLConnection, line: String) : List<String> {
        sendTo(connection, line)

        val result = receiveFrom(connection)

        if(result.isEmpty()) throw PastebinAPIException("No answer from the server")

        if(result[0].startsWith("Bad API request")) throw PastebinAPIException(result[0])

        return result
    }



    /**
     * Version of [query] with a map of [parameters]. These parameters are converted to a single [String] in POST format
     * with the [toPost] function.
     */
    private fun query(connection: URLConnection, parameters: Map<String, String>) =
        query(connection, toPost(parameters))



    /**
     * Version of [query] for connections with the [POST_URL]
     */
    private fun queryPost(parameters: Map<String, String>) = query(createPostConnection(), parameters)



    /**
     * Version of [query] with the [LOGIN_URL]
     */
    private  fun queryLogin(parameters: Map<String, String>) = query(createLoginConnection(), parameters)



    /**
     * Version of [query] with the [PRIVATE_RAW_URL]
     */
    private fun queryPrivateRaw(parameters: Map<String, String>) = query(createPrivateRawConnection(), parameters)



    /*
    QUERY PARAMETER FORMATTING. These take an API parameter and map it to its appropriate parameter name.
     */



    private fun option(option: String) = "api_option" to option

    private fun devKey(devKey: String) = "api_dev_key" to devKey

    private fun username(username: String) = "api_user_name" to username

    private fun password(password: String) = "api_user_password" to password

    private fun code(code: String) = "api_paste_code" to code

    private fun visibility(visibility: Int) = "api_paste_private" to visibility.toString()

    private fun name(name: String) = "api_paste_name" to name

    private fun expireDate(expireDate: String) = "api_expire_date" to expireDate

    private fun format(format: String) = "api_paste_format" to format

    private fun userKey(userKey: String) = "api_user_key" to userKey

    private fun resultsLimit(resultsLimit: String) = "api_results_limit" to resultsLimit

    private fun pasteKey(pasteKey: String) = "api_paste_key" to pasteKey



    /*
    QUERIES - The consumer endpoint.
     */



    /*
    1. api_dev_key - this is your API Developer Key.
    2. api_user_name - this is the username of the user you want to login.
    3. api_user_password - this is the password of the user you want to login.
     */



    /**
     * Queries the [LOGIN_URL] for a user key. The user key is required for any operations involving a Pastebin user.
     * This only needs to be generated once as it never expires. The user's [devKey], [username], and [password] are
     * required.
     */
    fun queryUserKey(devKey: String, username: String, password: String) = queryLogin(mapOf(
        devKey(devKey),
        username(username),
        password(password)))[0]



    /*
    Required:

    1. api_dev_key - which is your unique API Developers Key.
    2. api_option - set as paste, this will indicate you want to create a new paste.
    3. api_paste_code - this is the text that will be written inside your paste.

    Optional:

    1. api_user_key - this paramater is part of the login system, which is explained further down the page.
    2. api_paste_name - this will be the name / title of your paste.
    3. api_paste_format - this will be the syntax highlighting value, which is explained in detail further down the page.
    4. api_paste_private - this makes a paste public, unlisted or private, public = 0, unlisted = 1, private = 2
    5. api_paste_expire_date - this sets the expiration date of your paste, the values are explained futher down the page.
     */



    /**
     * Queries the [POST_URL] to create a new paste. The required parameters are the [devKey] and the paste [code],
     * which is just its contents. Any optional parameters can be excluded by substituting "null". Alternatively, users
     * can create variations of this method to suit their needs. This serves only as a basis for a paste query. If the
     * [userKey] is given, the paste will be created under the given user. If not, it is created anonymously. If the
     * [name] is not given, the paste will be untitled. If the [visibility] is not given, the paste will be public.
     * 0 = public, 1 = unlisted, 2 = private. The values that can be given for [expireDate] can be found on the Pastebin
     * API page. I don't know the default value for [expireDate] but giving it as 'N' will make it never expire. The
     * [format] determines how the paste will look on the Pastebin website. This is generally not a concern as
     * programmatic access to the API mostly queries the raw contents of the code. "text" will leave the format as raw
     * and is presumably the default option. The response from the server contains a URL to the paste if its creation
     * was successful.
     */
    fun queryPaste(devKey: String,
                   code: String,
                   name: String,
                   visibility: Int,
                   expireDate: String,
                   format: String,
                   userKey: String) = queryPost(mapOf(
        option("paste"),
        devKey(devKey),
        code(code),
        name(name),
        visibility(visibility),
        expireDate(expireDate),
        format(format),
        userKey(userKey)))[0]



    /*
    1. api_dev_key - this is your API Developer Key, in your case: YOUR API DEVELOPER KEY
    2. api_user_key - this is the session key of the logged in user. How to obtain such a key
    3. api_results_limit - this is not required, by default its set to 50, min value is 1, max value is 1000
    4. api_option - set as 'list'
     */



    /**
     * Queries the [POST_URL] for a list of all pastes and their metadata (but not their contents) created under a user.
     * This is given in XML format, which in this program is represented as a [List] of [String]s. This can be parsed
     * either by the user of by the [Paste] class, which provides some very messy but functional parsing methods and a
     * way of storing pastes in Kotlin. The user of this program is advised to create a Paste class suiting their needs.
     * Any valid [devKey] is required. The [userKey] of the user whose pastes are being queried is required. The
     * [resultsLimit] parameter is generally not important and is not required. It is the maximum number of pastes under
     * the user that will be returned. Its default value is 50, minimum value is 1, and max value is 1000.
     */
    fun queryList(devKey: String, userKey: String, resultsLimit: Int = 50) = queryPost(mapOf(
        devKey(devKey),
        userKey(userKey),
        resultsLimit(resultsLimit.toString()),
        option("list")))



    /**
     * Version of [queryList] that parses the XML response, returning a [List] of [Paste]s. The parsing method used in
     * this program is very messy, insecure, and not at all adherent to the basic principles of reusable code but it
     * somewhat works. The user is advised to create a Paste class and a parser to ensure stability and to suit their
     * needs.
     */
    fun queryListParsed(devKey: String, userKey: String, resultsLimit: Int = 50)
            = PasteParser.parsePastes(queryList(devKey, userKey, resultsLimit))



    /*
    1. api_dev_key - this is your API Developer Key, in your case: YOUR API DEVELOPER KEY
    2. api_user_key - this is the session key of the logged in user. How to obtain such a key
    3. api_paste_key - this is the unique key of the paste you want to delete.
    4. api_option - set as 'delete'
     */



    /**
     * Queries the [POST_URL] to delete a paste under a user. Any valid [devKey] is required. The [userKey] of the user
     * whose paste is being deleted is required. The [pasteKey] of the paste to be deleted is required. The response
     * simply states that the paste has been deleted, if the request was successful.
     */
    fun queryDelete(devKey: String, userKey: String, pasteKey: String) = queryPost(mapOf(
        devKey(devKey),
        userKey(userKey),
        pasteKey(pasteKey),
        option("delete")))



    /**
     * Version of [queryDelete] using the [pasteName] in place of the [pasteKey]. This method gets a list of all pastes
     * under the given user using the [queryList] method. It then finds the first paste that has the same name as the
     * given [pasteName]. This paste is then deleted. If a paste of the given name could not be found, a
     * [PastebinAPIException] is thrown. Returns the same result as [queryDelete].
     */
    fun queryDeleteGivenName(devKey: String, userKey: String, pasteName: String): List<String> {
        val paste = queryListParsed(devKey, userKey).firstOrNull { it.title == pasteName }

        if(paste != null)
            return queryDelete(devKey, userKey, paste.key)
        else
            throw PastebinAPIException("Could not delete the paste titled '$pasteName'. No such paste was found to exist.")
    }



    /*
    1. api_dev_key - this is your API Developer Key, in your case: 449ffb07a11bbb12c5d61eb3cc053b43
    2. api_user_key - this is the session key of the logged in user. How to obtain such a key
    3. api_paste_key - this is paste key you want to fetch the data from.
    4. api_option - set as 'show_paste'
     */



    /**
     * Queries the [PRIVATE_RAW_URL] for the raw contents of a private or public paste under the given user. Any valid
     * [devKey] is required. The [userKey] of the user whose paste is being queried is required. The [pasteKey] of the
     * paste is required. Note that if the paste is public, the [queryPublicRawCode] function is preferrable.
     */
    fun queryPrivateRawCode(devKey: String, userKey: String, pasteKey: String) = queryPrivateRaw(mapOf(
        devKey(devKey),
        userKey(userKey),
        pasteKey(pasteKey),
        option("show_paste")))



    /**
     * Accesses a URL formed from the [PUBLIC_RAW_URL] and the [pasteKey] to get the raw contents of any public paste.
     * This does not require any user information nor a valid dev key as it is not part of Pastebin's API. The format
     * of this URL is "https://pastebin.com/raw/pasteKey", with 'pasteKey' being substituted for the actual [pasteKey].
     */
    fun queryPublicRawCode(pasteKey: String) : List<String> {
        val url = URL(PUBLIC_RAW_URL + pasteKey)

        BufferedReader(InputStreamReader(url.openStream(), CHARSET)).use {
            return it.readLines()
        }
    }



    /**
     * Version of [queryPrivateRawCode] given the name of a paste as well as the information of the user who created
     * said paste. This queries the private code URL as it requires user details. Any valid [devKey] is required. The
     * [userKey] of the user whose paste is being queried is required. The [queryList] function is used to obtain a list
     * of the user's pastes. The first paste that has the given [pasteName] is the one whose contents are returned. If
     * no such paste matches the [pasteName], a [PastebinAPIException] is thrown.
     */
    fun queryRawCodeGivenName(devKey: String, userKey: String, pasteName: String) : List<String> {
        val paste = queryListParsed(devKey, userKey).firstOrNull { it.title == pasteName }

        if(paste != null)
            return queryPrivateRawCode(devKey, userKey, paste.key)
        else
            throw PastebinAPIException("Could not get the raw data of the private paste titled '$pasteName'. No such paste was found to exist for the given user.")
    }


}