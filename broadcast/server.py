import os
import aiohttp.web
import sys
import json

MONITOR_WWW = "../monitor/src/main/resources/www"

class LiveBroadcast:
    def __init__(self, path):
        with open(os.path.join(path, "static.json")) as f:
            self.static = json.load(f)

    async def live(self, req):
        ws = aiohttp.web.WebSocketResponse()
        await ws.prepare(req)

        await ws.send_json(self.static)

        return ws

def static(path):
    def handler(request):
        return aiohttp.web.FileResponse(os.path.join(os.path.dirname(__file__), path))
    return handler

def main(path):
    broadcast = LiveBroadcast(path)

    app = aiohttp.web.Application()
    app.router.add_route("GET", "/", static(os.path.join(MONITOR_WWW, "index.html")))
    app.router.add_route("GET", "/live/monitor", broadcast.live)
    app.router.add_static("/", MONITOR_WWW)
    aiohttp.web.run_app(app)

if __name__ == "__main__":
    main(sys.argv[1])
