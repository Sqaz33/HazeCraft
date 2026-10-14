"""Parse Source2Viewer text KV3 without gameplay-specific assumptions."""
import re, json, pathlib, argparse

TOKEN = re.compile(r'""".*?"""|<!--.*?-->|//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|[{}\[\]=,:]|[^\s{}\[\]=,:]+', re.S)

def loads(text):
    tokens = [m.group() for m in TOKEN.finditer(text) if not m.group().startswith(('<!--','//','/*'))]
    pos = 0
    def pop():
        nonlocal pos
        value = tokens[pos]
        pos += 1
        return value
    def value():
        nonlocal pos
        t = pop()
        if t == '{':
            result = {}
            while tokens[pos] != '}':
                if tokens[pos] == ',': pop(); continue
                key = pop()
                if key.startswith('"'): key = json.loads(key)
                assert pop() == '=', (key, tokens[pos-5:pos+5])
                assert key not in result, ('duplicate',key)
                result[key] = value()
            pop()
            return result
        if t == '[':
            result = []
            while tokens[pos] != ']':
                if tokens[pos] == ',': pop(); continue
                result.append(value())
            pop()
            return result
        if pos < len(tokens) and tokens[pos] == ':':
            pop()
            return value()
        if t.startswith('"""'): return t[3:-3].strip()
        if t.startswith('"'): return json.loads(t)
        if t in ('true','false'): return t == 'true'
        if t == 'null': return None
        try: return int(t, 0)
        except ValueError:
            try: return float(t)
            except ValueError: return t
    result = value()
    assert pos == len(tokens), tokens[pos:pos+10]
    return result

if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('input'); ap.add_argument('output')
    a = ap.parse_args()
    pathlib.Path(a.output).write_text(json.dumps(loads(pathlib.Path(a.input).read_text(encoding='utf8')), indent=2), encoding='utf8')
