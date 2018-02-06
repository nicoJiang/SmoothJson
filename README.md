# SmoothJson

## A fast json parser which needs just only one traversal of the raw json stream

With SmoothJson, raw json string can be processed in Python style, for example:

Provided that ` test.json ` contains content as below:

```json
{
    "array" : ["element", false, true, 666, {"key" : "value"}, ["2",3,4]],
    "map" : {
        "map_inner" : {
            "array_inner" : [2, 3, 5, false],
            "map_inner_inner" : {
                "key_inner_inner_1" : 233,
                "key_inner_inner_2" : "555"
            }
        },
        "string" : "my_string",
        "boolean" : true,
        "double" : 55.675,
        "long" : 8237283746374,
        "int" : 234
    },
    "other" : "ffggg"
}
```

And in python, you may use:

```python
import json

json_object = json.load(open("test.json"))
print json_object["map"]["map_inner"]["array_inner"][3]
```

Similarly, codes below will generate the same JSONObject:

```scala
import com.basic.json.SmoothJson

SmoothJson.load("test.json")
SmoothJson.getJSONObject().valueMap.get("map").valueMap.get("map_inner").valueMap.get("array_inner").valueArray(3).valueBoolean
```

Enjoy it!
