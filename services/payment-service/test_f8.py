import urllib.request, json, time

idem = 'idem-f8-' + str(int(time.time()))
body = json.dumps({
    'senderAccountId': '12345678901234',
    'receiverBankCode': '004',
    'receiverAccountNo': '12345678909999',
    'receiverHolderName': '홍판서',
    'transferAmount': 500000,
    'channel': 'MOBILE'
}).encode('utf-8')

req = urllib.request.Request(
    'http://localhost:8080/api/v1/payments',
    data=body,
    headers={
        'Content-Type': 'application/json; charset=utf-8',
        'X-Idempotency-Key': idem,
        'X-User-Id': 'user-001',
        'X-Auth-Token-Id': 'auth-f8-001'
    },
    method='POST'
)
try:
    with urllib.request.urlopen(req) as resp:
        print(json.dumps(json.loads(resp.read()), indent=2, ensure_ascii=False))
except urllib.error.HTTPError as e:
    print('HTTP', e.code, e.read().decode('utf-8'))
