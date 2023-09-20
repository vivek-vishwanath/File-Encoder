import java.io.File

fun main() {
    // The original JSON File
    val originalJSON = File("data\\input\\web-app.json")

    // The file that will contain the encoded json
    val encodedJSON = File("data\\output\\web-app.eson.txt")

    // The file that will contain the decoded version of the encoded json
    // The contents will be the same as the original JSON
    val decodedJSON = File("data\\output\\web-app.json")

    // Parse a json (as a File or a String) into an object to access its properties
    val json = JSON.parse(originalJSON)
    // Access the properties using "getter" syntax
    val taglibUri = json["web-app"]["taglib"]["taglib-uri"].value
    val betaServer = json["web-app"]["servlet"][4]["init-param"]["betaServer"].value
    println("web-app >> taglib >> taglib-uri = $taglibUri")
    println("web-app >> servlet >> [4] >> init-param >> betaServer = $betaServer")

    // Encode a json file into an "eson" file
    JSON.encode(originalJSON, encodedJSON)

    // Decode an "eson" file back into a json file
    JSON.decode(encodedJSON, decodedJSON)
}
