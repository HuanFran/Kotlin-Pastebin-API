package main

/**
 * Bundles the [devKey] and [userKey] of a user together to make calling API functions easier. Can be created in the
 * [PastebinAPI] object or with this constructor. The [userKey] never expires unless it is queried from the server, so
 * it can be cached at the start of every session. However, if multiple devices are using the same user key, problems
 * may arise if one user attempts to query the server for a new key. In this case, it is better to obtain the user key
 * once and never query the server for another one. This user key can be stored in-code.
 *
 * @see PastebinAPI.createContext
 */
class PastebinContext(val devKey: String, val userKey: String) {


    /**
     * @see PastebinAPI.queryPaste
     */
    fun queryPaste(code: String, name: String, visibility: Int, expireDate: String, format: String) : String =
        PastebinAPI.queryPaste(
            devKey,
            code,
            name,
            visibility,
            expireDate,
            format,
            userKey)



    /**
     * Version of the [queryPaste] method with only the paste [code] and [name] as parameters. The visibility is set to
     * private, the expire date to never and the format to "text".
     *
     * @see PastebinAPI.queryPaste
     */
    fun queryPaste(code: String, name: String) : String = queryPaste(code, name, 2, "N", "text")



    /**
     * @see PastebinAPI.queryList
     */
    fun queryList(resultsLimit: Int = 50) : List<String> = PastebinAPI.queryList(
        devKey,
        userKey,
        resultsLimit)



    /**
     * @see PastebinAPI.queryListParsed
     */
    fun queryListParsed(resultsLimit: Int = 50) : List<Paste>
            = PastebinAPI.queryListParsed(devKey, userKey, resultsLimit)



    /**
     * @see PastebinAPI.queryDelete
     */
    fun queryDelete(pasteKey: String) = PastebinAPI.queryDelete(
        devKey,
        userKey,
        pasteKey)



    /**
     * @see PastebinAPI.queryDeleteGivenName
     */
    fun queryDeleteGivenName(pasteName: String) =
        PastebinAPI.queryDeleteGivenName(devKey, userKey, pasteName)



    /**
     * @see PastebinAPI.queryPrivateRawCode
     */
    fun queryPrivateRawCode(pasteKey: String) = PastebinAPI.queryPrivateRawCode(
        devKey,
        userKey,
        pasteKey)



    /**
     * No difference as this method requires neither [devKey] nor [userKey]. Here only for convenience and continuity.
     *
     * @see PastebinAPI.queryPublicRawCode
     */
    fun queryPublicRawCode(pasteKey: String) = PastebinAPI.queryPublicRawCode(pasteKey)



    /**
     * @see PastebinAPI.queryRawCodeGivenName
     */
    fun queryRawCodeGivenName(pasteName: String) =
        PastebinAPI.queryRawCodeGivenName(devKey, userKey, pasteName)


}