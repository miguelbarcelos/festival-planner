import unittest

from hellfest_parser import Token, parse_tokens


class ParserTest(unittest.TestCase):
    def test_normal_same_day_set(self):
        sets, warnings = parse_tokens(
            [
                Token("date", "18/6/26"),
                Token("stage", "MAINSTAGE 1"),
                Token("artist", "WE CAME AS ROMANS"),
                Token("time", "16:30 - 17:10"),
            ]
        )
        self.assertEqual("2026-06-18T16:30:00+02:00", sets[0]["start"])
        self.assertEqual("2026-06-18T17:10:00+02:00", sets[0]["end"])
        self.assertEqual([], warnings)

    def test_after_midnight_set(self):
        sets, _ = parse_tokens(
            [
                Token("date", "18/6/26"),
                Token("stage", "MAINSTAGE 1"),
                Token("artist", "HEADLINER"),
                Token("time", "23:30 - 01:00"),
            ]
        )
        self.assertEqual("2026-06-19T01:00:00+02:00", sets[0]["end"])

    def test_early_morning_start_belongs_to_next_calendar_day(self):
        sets, _ = parse_tokens(
            [
                Token("date", "18/6/26"),
                Token("stage", "WARZONE"),
                Token("artist", "LATE SET"),
                Token("time", "01:05 - 02:05"),
            ]
        )
        self.assertEqual("2026-06-19T01:05:00+02:00", sets[0]["start"])

    def test_multiple_stages(self):
        sets, _ = parse_tokens(
            [
                Token("date", "18/6/26"),
                Token("stage", "ALTAR"),
                Token("artist", "A"),
                Token("time", "18:00 - 19:00"),
                Token("stage", "TEMPLE"),
                Token("artist", "B"),
                Token("time", "18:00 - 19:00"),
            ]
        )
        self.assertEqual(["ALTAR", "TEMPLE"], [item["stage"] for item in sets])

    def test_malformed_time_is_reported(self):
        sets, warnings = parse_tokens(
            [
                Token("date", "18/6/26"),
                Token("stage", "ALTAR"),
                Token("artist", "A"),
                Token("time", "later"),
            ]
        )
        self.assertEqual([], sets)
        self.assertEqual(1, len(warnings))


if __name__ == "__main__":
    unittest.main()
