import os
import aiohttp.web

MONITOR_WWW = "../monitor/src/main/resources/www"

def static(path):
    def handler(request):
        return aiohttp.web.FileResponse(os.path.join(os.path.dirname(__file__), path))
    return handler

def main():
    app = aiohttp.web.Application()
    app.router.add_route("GET", "/", static(os.path.join(MONITOR_WWW, "index.html")))
    app.router.add_static("/", MONITOR_WWW)
    aiohttp.web.run_app(app)

if __name__ == "__main__":
    main()
