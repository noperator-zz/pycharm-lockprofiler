import json
import sys

from line_profiler import load_stats


UNSUPPORTED_PICKLE_PROTOCOL_5_EXITCODE = 2


if __name__ == '__main__':
    profile_input_file = sys.argv[1]
    profile_output_json = sys.argv[2]

    try:
        obj = load_stats(profile_input_file)
    except ValueError as exc:
        if str(exc) == "unsupported pickle protocol: 5":
            sys.exit(2)
        raise exc

    dict_stats = {
        "profiledFunctions": [{
            "file": key[0],
            "lineNo": key[1],
            "functionName": key[2],
            "profiledLines": [{
                "lineNo": element[0],
                "hits": element[1],
                "time": element[2]
            } for element in value]
        } for key, value in obj.timings.items()],
        "unit": obj.unit
    }

    with open(profile_output_json, 'w') as fp:
        json.dump(dict_stats, fp)

