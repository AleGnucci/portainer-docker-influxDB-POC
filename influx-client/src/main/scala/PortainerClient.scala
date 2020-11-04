import com.google.gson.JsonParser
import sttp.client3.{HttpURLConnectionBackend, Identity, Request, Response, SttpBackend, UriContext, basicRequest}

class PortainerClient {

  private val backend = HttpURLConnectionBackend()
  private val baseUrl = "http://portainer:9000/api"

  def useAPIs(): Unit = {
      createUser()
      val jwt = loginAngGetJwt()
      val endpointId = createLocalEndpointAndGetId(jwt)
      val imageToPull = "nginx:latest"
      pullImage(jwt, endpointId, imageToPull)
      val containerId = createContainerAndGetId(jwt, endpointId, imageToPull)
      startOrStopContainer(jwt, endpointId, containerId, containerShouldRun = true)
      Thread.sleep(2000) // wait before deleting the container
      startOrStopContainer(jwt, endpointId, containerId, containerShouldRun = false)
      deleteContainer(jwt, endpointId, containerId)
    }

  private def createUser() =
    sendRequest(adminRequest.post(uri"$baseUrl/users/admin/init"))

  private val adminRequest = basicRequest.body("""{"Username":"admin", "Password":"adminpassword"}""")

  private def sendRequest(request: Request[Either[String, String], Any]) =
    request.contentType("application/json").send(backend)

  private def loginAngGetJwt(): String = {
    val request = adminRequest.post(uri"$baseUrl/auth")
    getStringFromResponse(sendRequest(request), "jwt")
  }

  private def getStringFromResponse(response: Response[Either[String, String]], field: String): String = {
    if(response.body.isLeft) println("Portainer responded with " + response)
    JsonParser.parseString(response.body.toOption.get).getAsJsonObject.get(field).getAsString
  }

  private def createLocalEndpointAndGetId(jwt: String): Int = {
    val request = authorizedRequest(jwt).post(uri"$baseUrl/endpoints?Name=test-local&EndpointType=1")
    getStringFromResponse(sendRequest(request), "Id").toInt
  }

  private def authorizedRequest(jwt: String) =
    basicRequest.header("Authorization", s"Bearer $jwt")

  private def pullImage(jwt: String, endpointId: Int, imageName: String) = {
    sendRequest(authorizedRequest(jwt)
      .post(uri"$baseUrl/endpoints/$endpointId/docker/images/create?fromImage=$imageName"))
  }

  private def createContainerAndGetId(jwt: String, endpointId: Int, imageName: String): String = {
    val request = authorizedRequest(jwt).body(s"""{"name":"web01", "Image":"$imageName"}""")
      .post(uri"${getContainersUrl(endpointId)}/create")
    getStringFromResponse(sendRequest(request), "Id")
  }

  private def getContainersUrl(endpointId: Int): String = s"$baseUrl/endpoints/$endpointId/docker/containers"

  private def startOrStopContainer(jwt: String, endpointId: Int, containerId: String, containerShouldRun: Boolean) = {
    val startOrStop = if(containerShouldRun) "start" else "stop"
    sendRequest(authorizedRequest(jwt).post(uri"${getContainerHandlingUrl(endpointId, containerId)}/$startOrStop"))
  }

  private def getContainerHandlingUrl(endpointId: Int, containerId: String): String =
    s"${getContainersUrl(endpointId)}/$containerId"

  private def deleteContainer(jwt: String, endpointId: Int, containerId: String): Unit = {
    val request = authorizedRequest(jwt).delete(uri"${getContainerHandlingUrl(endpointId, containerId)}")
    val response = sendRequest(request)
    if(!response.isSuccess) throw new IllegalStateException("Error when deleting container: " + response)
    println(response) // prints a 204 success status if it's all ok
  }

}

object PortainerClient {
  def apply(): PortainerClient = new PortainerClient()
}
