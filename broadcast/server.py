import asyncio
import aiohttp.web
import json
import os
import sys

MONITOR_WWW = "../monitor/src/main/resources/www"

class LiveBroadcast:
    def __init__(self, path):
        self.path = path
        self.dynamic = []
        self.step = 0
        self.condition = asyncio.Condition()

        with open(os.path.join(path, "static.json")) as f:
            self.static = json.load(f)

        for step in range(self.static["steps"]):
            if step % 5 == 0:
                with open(os.path.join(path, f"{step}.json")) as f:
                    group = json.load(f)
            self.dynamic.append(group[str(step)])

    async def run(self):
        for step in range(self.static["steps"]):
            self.step = step
            print(self.path, self.step, "/", self.static["steps"] - 1)

            async with self.condition:
                self.condition.notify_all()

            await asyncio.sleep(0.1)

    async def live(self, req):
        ws = aiohttp.web.WebSocketResponse()
        await ws.prepare(req)

        await ws.send_json(self.static)
        await ws.send_json(self.dynamic[self.step])

        while True:
            async with self.condition:
                await self.condition.wait()
                await ws.send_json(self.dynamic[self.step])

        return ws

def static(path):
    def handler(request):
        return aiohttp.web.FileResponse(os.path.join(os.path.dirname(__file__), path))
    return handler

async def main(path):
    broadcast = LiveBroadcast(path)

    asyncio.create_task(broadcast.run())

    app = aiohttp.web.Application()
    app.router.add_route("GET", "/", static(os.path.join(MONITOR_WWW, "index.html")))
    app.router.add_route("GET", "/live/monitor", broadcast.live)
    app.router.add_static("/", MONITOR_WWW)
    return app

if __name__ == "__main__":
    aiohttp.web.run_app(main(sys.argv[1]))
