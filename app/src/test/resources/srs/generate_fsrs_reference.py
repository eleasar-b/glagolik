# Generates app/src/test/resources/fsrs-reference.csv with py-fsrs 6.3.2.
from datetime import datetime, timedelta, timezone
from fsrs import Scheduler, Card, Rating
s = Scheduler(enable_fuzzing=False)
t0 = datetime(2026, 1, 1, 8, 0, tzinfo=timezone.utc)
R = {'A': Rating.Again, 'H': Rating.Hard, 'G': Rating.Good, 'E': Rating.Easy}
# (rating, gap in days since previous review; None = review exactly when due)
scenarios = {
  "learn_good_then_reviews": [('G',0),('G',1/1440),('G',None),('G',None),('A',None),('G',10/1440),('G',None)],
  "easy_first": [('E',0),('E',None),('H',None)],
  "hard_steps": [('H',0),('H',5.5/1440),('G',3/1440),('G',10/1440),('G',None)],
  "again_loop": [('A',0),('A',1/1440),('G',1/1440),('G',10/1440)],
  "late_review": [('G',0),('G',10/1440),('G',30),('G',3),('A',50),('H',10/1440),('G',0.5)],
}
print("scenario,rating,reviewed_at,state,step,stability,difficulty,due")
for name, steps in scenarios.items():
    card = Card(card_id=1, due=t0)
    now = t0
    for r, gap in steps:
        now = card.due if gap is None else now + timedelta(days=gap)
        card, _ = s.review_card(card, R[r], review_datetime=now)
        step = "" if card.step is None else card.step
        print(f"{name},{R[r].name.upper()},{int(now.timestamp())},{card.state.name.upper()},{step},{card.stability!r},{card.difficulty!r},{int(card.due.timestamp())}")
