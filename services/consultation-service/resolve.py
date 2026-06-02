import re, sys

path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

def resolve_head(m):
    return m.group(1)

result = re.sub(
    r'<<<<<<< HEAD\n(.*?)=======\n.*?>>>>>>> origin/main\n',
    resolve_head,
    content,
    flags=re.DOTALL
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(result)
print('resolved:', path)
