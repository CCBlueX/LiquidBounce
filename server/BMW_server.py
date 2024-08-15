import websockets
from asyncio import run as run_async, Future
from json import dumps, loads

users = []
FREE = True

async def handler(websocket, path):
    try:
        async for message in websocket:
            print(f"ip: {websocket.remote_address[0]}")
            print(f"message: {message}")
            print()
            messageJson = loads(message)
            if (messageJson["func"] == "create_user"):
                for user in users:
                    if (user["server"] == messageJson["server"]):
                        await user["websocket"].send(dumps({
                            "func": "create_user",
                            "name": messageJson["name"]
                        }))
                        await websocket.send(dumps({
                            "func": "create_user",
                            "name": user["name"]
                        }))
                users.append({
                    "server": messageJson["server"],
                    "name": messageJson["name"],
                    "websocket": websocket
                })

            elif (messageJson["func"] == "send_msg"):
                for user in users:
                    await user["websocket"].send(dumps({
                        "func": "send_msg",
                        "name": user["name"],
                        "msg": messageJson["msg"]
                    }))

            elif (messageJson["func"] == "get_free"):
                await websocket.send(dumps({
                    "func": "get_free",
                    "free": FREE
                }))

    except websockets.ConnectionClosed:
        for user in users:
            if (user["websocket"] == websocket):
                name = user["name"]
                users.remove(user)
                break
        for user in users:
            await user["websocket"].send(dumps({
                "func": "remove_user",
                "name": name
            }))

async def main():
    async with websockets.serve(handler, "0.0.0.0", 12345):
        print("BMW Server Started")
        print()
        await Future()

if (__name__ == '__main__'):
    run_async(main())
