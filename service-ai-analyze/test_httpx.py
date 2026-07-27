import httpx

# Try with custom transport that doesn't pool connections
transport = httpx.HTTPTransport(retries=0, proxy=None)
with httpx.Client(timeout=30.0, transport=transport) as client:
    resp = client.post(
        'http://localhost:11434/api/chat',
        json={'model':'qwen2.5:7b','messages':[{'role':'user','content':'hello'}],'stream':False}
    )
    print(f"Status: {resp.status_code}")
    print(f"Body: {resp.text[:300]}")
