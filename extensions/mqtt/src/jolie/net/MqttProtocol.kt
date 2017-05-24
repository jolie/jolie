package jolie.net

import java.io.IOException
import java.io.OutputStream
import java.io.InputStream

/**
 * Released Under Creative Common License
 * Created by stefanopiozingaro on 23/05/17.
 */

class MqttProtocol() {

    @Throws(IOException::class)
    fun send(ostream: OutputStream, message: CommMessage, istream: InputStream) {

    }

    companion object {
        private val INITIAL_CAPACITY = 8
    }
}
