package care.bima.document.storage

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray

class ObjectStorageClient(
    endpoint: String = System.getenv("MINIO_ENDPOINT") ?: "http://localhost:9000",
    accessKey: String = System.getenv("MINIO_ACCESS_KEY") ?: "bima",
    secretKey: String = System.getenv("MINIO_SECRET_KEY") ?: "bima-dev-only",
    private val bucket: String = System.getenv("MINIO_BUCKET") ?: "bima-documents",
) {
    private val client =
        S3Client {
            endpointUrl = aws.smithy.kotlin.runtime.net.url.Url.parse(endpoint)
            region = "us-east-1"
            forcePathStyle = true
            credentialsProvider =
                StaticCredentialsProvider(Credentials(accessKeyId = accessKey, secretAccessKey = secretKey))
        }

    suspend fun upload(
        key: String,
        contentType: String,
        bytes: ByteArray,
    ) {
        client.putObject(
            PutObjectRequest {
                bucket = this@ObjectStorageClient.bucket
                this.key = key
                this.contentType = contentType
                body = ByteStream.fromBytes(bytes)
            },
        )
    }

    suspend fun download(key: String): ByteArray {
        var result: ByteArray? = null
        client.getObject(
            GetObjectRequest {
                bucket = this@ObjectStorageClient.bucket
                this.key = key
            },
        ) { response ->
            result = response.body?.toByteArray() ?: ByteArray(0)
        }
        return result ?: ByteArray(0)
    }
}
