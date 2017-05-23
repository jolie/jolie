package jolie.net

/**
 * Released Under Creative Common License
 * Created by stefanopiozingaro on 23/05/17.
 */

/**
 * TODO integrate kotlin and mqtt module into the ant build.xml
*/

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.URI
import java.util.HashMap
import jolie.Interpreter
import jolie.net.http.HttpMessage
import jolie.net.http.HttpParser
import jolie.net.http.HttpUtils
import jolie.net.http.Method
import jolie.net.http.UnsupportedMethodException
import jolie.net.protocols.SequentialCommProtocol
import jolie.runtime.ByteArray
import jolie.runtime.FaultException
import jolie.runtime.Value
import jolie.runtime.VariablePath
import jolie.runtime.typing.Type
import jolie.js.JsUtils

class MqttProtocol(configurationPath: VariablePath, private val uri: URI,
                      private val interpreter: Interpreter, private val inInputPort: Boolean) : SequentialCommProtocol(configurationPath), HttpUtils.HttpProtocol {
    private var encoding: String? = null

    private val jsonRpcIdMap: MutableMap<Long, String>
    private val jsonRpcOpMap: MutableMap<String, String>

    override fun name(): String {
        return "jsonrpc"
    }

    init {

        // prepare the two maps
        this.jsonRpcIdMap = HashMap<Long, String>(INITIAL_CAPACITY, LOAD_FACTOR)
        this.jsonRpcOpMap = HashMap<String, String>(INITIAL_CAPACITY, LOAD_FACTOR)
    }

    @Throws(IOException::class)
    fun send_internal(ostream: OutputStream, message: CommMessage, istream: InputStream) {
        channel().setToBeClosed(!checkBooleanParameter("keepAlive", true))

        if (!message.isFault() && message.hasGenericId() && inInputPort) {
            // JSON-RPC notification mechanism (method call with dropped result)
            // we just send HTTP status code 204
            val httpMessage = StringBuilder()
            httpMessage.append("HTTP/1.1 204 No Content" + HttpUtils.CRLF)
            httpMessage.append("Server: Jolie" + HttpUtils.CRLF + HttpUtils.CRLF)
            ostream.write(httpMessage.toString().toByteArray(charset("utf-8")))
            return
        }

        val value = Value.create()
        value.getFirstChild("jsonrpc").setValue("2.0")
        if (message.isFault()) {
            val error = value.getFirstChild("error")
            error.getFirstChild("code").setValue(-32000)
            error.getFirstChild("message").setValue(message.fault().faultName())
            error.getChildren("data").set(0, message.fault().value())
            val jsonRpcId = jsonRpcIdMap[message.id()]
            error.getFirstChild("id").setValue(jsonRpcId)
        } else {
            if (inInputPort) {
                value.getChildren("result").set(0, message.value())
                val jsonRpcId = jsonRpcIdMap[message.id()]
                value.getFirstChild("id").setValue(jsonRpcId)
            } else {
                jsonRpcOpMap.put(message.id() + "", message.operationName())
                value.getFirstChild("method").setValue(message.operationName())
                if (message.value().isDefined() || message.value().hasChildren()) {
                    // some implementations need an array here
                    value.getFirstChild("params").getChildren(JsUtils.JSONARRAY_KEY).set(0, message.value())
                }
                value.getFirstChild("id").setValue(message.id())
            }
        }
        val json = StringBuilder()
        JsUtils.valueToJsonString(value, true, Type.UNDEFINED, json)
        var content = ByteArray(json.toString().toByteArray(charset("utf-8")))

        val httpMessage = StringBuilder()
        if (inInputPort) {
            // We're responding to a request
            httpMessage.append("HTTP/1.1 200 OK" + HttpUtils.CRLF)
            httpMessage.append("Server: Jolie" + HttpUtils.CRLF)
        } else {
            // We're sending a request
            var path: String? = uri.rawPath // TODO: fix this to consider resourcePaths
            if (path == null || path.length == 0) {
                path = "*"
            }
            httpMessage.append("POST " + path + " HTTP/1.1" + HttpUtils.CRLF)
            httpMessage.append("User-Agent: Jolie" + HttpUtils.CRLF)
            httpMessage.append("Host: " + uri.host + HttpUtils.CRLF)

            if (checkBooleanParameter("compression", true)) {
                val requestCompression = getStringParameter("requestCompression")
                if (requestCompression == "gzip" || requestCompression == "deflate") {
                    encoding = requestCompression
                    httpMessage.append("Accept-Encoding: " + encoding + HttpUtils.CRLF)
                } else {
                    httpMessage.append("Accept-Encoding: gzip, deflate" + HttpUtils.CRLF)
                }
            }
        }

        if (channel().toBeClosed()) {
            httpMessage.append("Connection: close" + HttpUtils.CRLF)
        }

        if (encoding != null && checkBooleanParameter("compression", true)) {
            content = HttpUtils.encode(encoding, content, httpMessage)
        }

        //httpMessage.append( "Content-Type: application/json-rpc; charset=utf-8" + HttpUtils.CRLF );
        httpMessage.append("Content-Type: application/json; charset=utf-8" + HttpUtils.CRLF)
        httpMessage.append("Content-Length: " + content.size() + HttpUtils.CRLF + HttpUtils.CRLF)

        if (checkBooleanParameter("debug", false)) {
            interpreter.logInfo("[JSON-RPC debug] Sending:\n" + httpMessage.toString() + content.toString("utf-8"))
        }

        ostream.write(httpMessage.toString().toByteArray(charset(HttpUtils.URL_DECODER_ENC)))
        ostream.write(content.getBytes())
    }

    @Throws(IOException::class)
    override fun send(ostream: OutputStream, message: CommMessage, istream: InputStream) {
        HttpUtils.send(ostream, message, istream, inInputPort, channel(), this)
    }

    @Throws(IOException::class)
    fun recv_internal(istream: InputStream, ostream: OutputStream): CommMessage? {
        val parser = HttpParser(istream)
        val message = parser.parse()
        val charset = HttpUtils.getCharset(null, message)
        HttpUtils.recv_checkForChannelClosing(message, channel())

        if (message.isError()) {
            throw IOException("HTTP error: " + String(message.content(), charset))
        }
        if (inInputPort && message.type() != HttpMessage.Type.POST) {
            throw UnsupportedMethodException("Only HTTP method POST allowed!", Method.POST)
        }

        encoding = message.getProperty("accept-encoding")

        val value = Value.create()
        if (message.size() > 0) {
            if (checkBooleanParameter("debug", false)) {
                interpreter.logInfo("[JSON-RPC debug] Receiving:\n" + String(message.content(), charset))
            }

            JsUtils.parseJsonIntoValue(InputStreamReader(ByteArrayInputStream(message.content()), charset), value, false)

            if (!value.hasChildren("id")) {
                // JSON-RPC notification mechanism (method call with dropped result)
                if (!inInputPort) {
                    throw IOException("A JSON-RPC notification (message without \"id\") needs to be a request, not a response!")
                }
                return CommMessage(CommMessage.GENERIC_ID, value.getFirstChild("method").strValue(),
                        "/", value.getFirstChild("params"), null)
            }
            val jsonRpcId = value.getFirstChild("id").strValue()
            if (inInputPort) {
                jsonRpcIdMap.put(jsonRpcId.hashCode().toLong(), jsonRpcId)
                return CommMessage(jsonRpcId.hashCode().toLong(), value.getFirstChild("method").strValue(),
                        "/", value.getFirstChild("params"), null)
            } else if (value.hasChildren("error")) {
                val operationName = jsonRpcOpMap[jsonRpcId]
                return CommMessage(java.lang.Long.valueOf(jsonRpcId)!!, operationName, "/", null,
                        FaultException(
                                value.getFirstChild("error").getFirstChild("message").strValue(),
                                value.getFirstChild("error").getFirstChild("data")
                        )
                )
            } else {
                // Certain implementations do not provide a result if it is "void"
                val operationName = jsonRpcOpMap[jsonRpcId]
                return CommMessage(java.lang.Long.valueOf(jsonRpcId)!!, operationName, "/", value.getFirstChild("result"), null)
            }
        }
        return null // error situation
    }

    @Throws(IOException::class)
    override fun recv(istream: InputStream, ostream: OutputStream): CommMessage {
        return HttpUtils.recv(istream, ostream, inInputPort, channel(), this)
    }

    companion object {

        private val INITIAL_CAPACITY = 8
        private val LOAD_FACTOR = 0.75f
    }
}
