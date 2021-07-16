def find_max_score(i, remain_time, places, memo):
    '''O(nm) time & O(nm) space solution'''
    if i >= len(places):
        return 0
    if (i, remain_time) in memo:
        return memo[(i, remain_time)]
    result = find_max_score(i + 1, remain_time, places, memo)
    duration, score = places[i]
    if duration <= remain_time:
        cand = find_max_score(i + 1, remain_time - duration, places, memo) + score
        result = max(cand, result)
    memo[(i, remain_time)] = result
    return result


def main():
    n, m = map(int, input().strip().split(' '))
    places = []
    for _ in range(n):
        _, duration, score = input().strip().split(',')
        places.append((int(duration), int(score)))
    result = find_max_score(0, m, places, {})
    print(result)


def test():
    m = 5
    places = [(2, 5), (5, 20), (1, 10), (3, 30)]
    result = find_max_score(0, m, places, {})
    expected = 40
    assert result == expected
    m = 3
    places = [(2, 5), (5, 20), (1, 10), (3, 30)]
    result = find_max_score(0, m, places, {})
    expected = 30
    assert result == expected


if __name__ == '__main__':
    test()
    main()
