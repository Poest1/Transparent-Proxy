# Transparent-Proxy



Support the following HTTP methods; GET, HEAD,
OPTIONS, and POST.
 If the proxy encounters any other requested method from the client, it
should return a “405 Method Not Allowed” response without contacting the
requested domain.
 If the proxy encounters a GET, HEAD, OPTIONS or POST request with a
filtered domain, it should return a “401 Unauthorized” response without
contacting the requested domain.
 Your proxy server should be able to cache proper resources on the disk
when clients access websites. The most common way to determine if a
resource is cacheable is to check if the HTTP response from the server
 The proxy server should save
these resources along with their URL addresses and last modification dates.
 When a client requests the same resource again in the future, the
proxy server should query the web server with HTTP request
containing “If-Modified-Since” header to check whether the cached
resource is expired or not.
 If the resource is not expired, then the proxy server can serve the
resource back to the client using its cache. Otherwise, it should
download the recent version of the resource and relay it to the
requesting client. Also update its cache with the latest version.
 Proxy server  support downloading large files (> 500 MB) when
requested by the client
