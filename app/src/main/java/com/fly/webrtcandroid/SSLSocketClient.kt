package com.fly.webrtcandroid


import java.security.SecureRandom
import javax.net.ssl.*

object SSLSocketClient{

    //获取这个SSLSocketFactory
    fun getSSLSocketFactory(): SSLSocketFactory {
        return try {
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, getTrustManager(), SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    val x509TrustManager: X509TrustManager by lazy {
        object : X509TrustManager {

            override fun checkClientTrusted(
                chain: Array<out java.security.cert.X509Certificate>?,
                authType: String?
            ) {

            }

            override fun checkServerTrusted(
                chain: Array<out java.security.cert.X509Certificate>?,
                authType: String?
            ) {

            }


            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }

        }
    }

    //获取TrustManager
    private fun getTrustManager(): Array<TrustManager>? {
        return arrayOf<TrustManager>(
            x509TrustManager
        )
    }

    //获取HostnameVerifier
    fun getHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { s, sslSession -> true }
    }

}