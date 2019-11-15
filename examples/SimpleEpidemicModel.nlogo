;; ------------------------------------------------------------------------------------------
;; SimpleEpidemicModel.nlogo
;;
;; This is a simple model of an epidemic outbreak and a policy to contain it. Its main aim
;; is to demonstrate the use of the factbase extension.
;;
;; Copyright (c) 2016 Centre for Policy Modelling
;;
;; Author: Ruth Meyer (ruth@cfpm.org)
;;
;; ------------------------------------------------------------------------------------------

extensions [factbase]

breed [people person]

people-own [
  age
  calendar     ;; my schedule as a factbase
  my-home      ;; where I live
  my-work      ;; my workplace (if an adult and employed)
  my-school    ;; my school (if a school child)
  compulsory   ;; my compulsory activity (work/school/home)
  activities   ;; my leisure activities (1-n of the possible activity places)
  status       ;; the health status
  offset       ;; x- and y offset to spread turtles around on patches a bit
]

patches-own [
  function     ;; what people can do here
  hh-size      ;; if it's a household, this records its size
  hh-members   ;; if it's a household, records the people living here
]

globals [
  place-functions place-numbers place-colours     ;; defining the different locations
  hh-size-mean hh-size-sd age-dist age-interval   ;; defining the population
  unemployment-rate health-status health-colours  ;;
  ticks-per-day durations                         ;; defining general time-related features
  global-health                                   ;; a factbase keeping track of when everyone's health status changes
  incubation sick-to-death-time sick-to-well-time ;; defining the epidemic's time-related features
  vaccination-started? people-per-tick            ;; defining vaccination policy related stuff
  count-infected-at                               ;; keeps track of how many people get infected at home/work/school/activity
]

;; ---------------------------------------------------------------------------------------------------------------------
;; Model setup
;; ---------------------------------------------------------------------------------------------------------------------

to setup
  clear-all

  ;; set global variables
  set place-functions ["home" "work" "school" "activity" "hospital"]
  set place-colours [gray black brown blue white]
  set place-numbers [25 3 1 6 1]
  set hh-size-mean 4
  set hh-size-sd 2
  set age-dist [0.21 0.12 0.07 0.25 0.18 0.14 0.03]
  set age-interval [0 14 24 29 44 59 79 99]
  set unemployment-rate 0.2
  set health-status ["healthy" "infected" "sick" "immune" "dead"]
  set health-colours [green pink red orange violet]
  set ticks-per-day 8
  set durations [-1 3 2 1 -1] ;; in order of place functions, with -1 representing indefinite
  set global-health factbase:create ["person" "status" "timestamp"]
  set incubation 7 * ticks-per-day ;; incubation period is 7 days
  set sick-to-death-time 3 * ticks-per-day ;; with probability death-rate sick people die after 3 days
  set sick-to-well-time 5 * ticks-per-day ;; if they don't die they get well again after 5 days
  set vaccination-started? false
  set people-per-tick 25 ;; how many people can be vaccinated per tick
  set count-infected-at [0 0 0 0]

  ;; set up places
  setup-places
  ;; create people in households
  create-households

  reset-ticks

  ;; pick some initially infected people
  ask n-of num-initial-infected people [
    turn "infected"
  ]
end

to setup-places
  ;; assign functions to patches (number of patches per function determined by values in place-numbers)
  foreach n-values (length place-functions) [ i -> i ] [ i ->
    let counter 0
    ask n-of (item i place-numbers) (patches with [function = 0]) [
      set function item i place-functions
      set pcolor item i place-colours
      if (not member? function ["home" "hospital"]) [
        set plabel (word (first function) "-" counter)
        set counter counter + 1
      ]
      if (function = "hospital") [
        set plabel-color red
        set plabel "hospital"
      ]
    ]
  ]
end

to create-households
  ;; each "home" patch is a household
  ask patches with [function = "home"] [
    ;;determine size of household
    set hh-size ceiling (random-normal hh-size-mean hh-size-sd)
    while [hh-size < 1] [
      set hh-size ceiling (random-normal hh-size-mean hh-size-sd)
    ]
    set hh-members []
  ]
  ;; create people according to age distribution
  let num-agents sum [hh-size] of patches
  let offset-sign [-1 1]
  create-people num-agents [
    let index sample-empirical-distribution age-dist (n-values (length age-dist) [ i -> i ])
    let start-age (item index age-interval)
    set age start-age + random ((item (index + 1) age-interval) - start-age)
    set size age / 100 * 0.5
    set shape "circle"
    set status 0 ;; healthy
    set color item status health-colours
    set calendar factbase:create ["start" "activity" "location"]
    set offset (list ((item (random 2) offset-sign) * (random-float 0.32)) ((item (random 2) offset-sign) * (random-float 0.32)))
    set compulsory "home" ;; default is stay-at-home
  ]
  ;; assign people to households
  ;; starting with adults
  let adults people with [age > 18 and age <= 65]
  ask adults [
    ;; pick an available household
    set my-home one-of patches with [function = "home" and length hh-members < hh-size]
    ask my-home [
      set hh-members lput myself hh-members
    ]
    ;; pick a work place
    if (random-float 1.0 > unemployment-rate) [
      set my-work one-of patches with [function = "work"]
      set compulsory "work"
    ]
  ]
  let children people with [age <= 18]
  ask children [
    ;; pick an available household with at least 1 adult
    set my-home one-of patches with [function = "home" and length hh-members > 0 and length hh-members < hh-size]
    ask my-home [
      set hh-members lput myself hh-members
    ]
    ;; pick a school
    if (age > 4) [
      set my-school one-of patches with [function = "school"]
      set compulsory "school"
    ]
  ]
  let seniors people with [age > 65]
  ask seniors [
    ;; pick any still available household
    set my-home one-of patches with [function = "home" and length hh-members < hh-size]
    ask my-home [
      set hh-members lput myself hh-members
    ]
  ]
  ask people [
    ;; pick some random activities
    pick-activities
    ;; go home
    travel-to my-home
  ]
end

;; ---------------------------------------------------------------------------------------------------------------------
;; Model processes
;; ---------------------------------------------------------------------------------------------------------------------


to go

  ;; at the start of the day, people (who are not sick) plan their schedule
  if (ticks mod ticks-per-day = 0) [
    ask people with [not (is-sick? or is-dead?)] [ plan-schedule ]
  ]

  ;; everyone checks their health status
  ask people [ check-health ]

  ;; they do their current activity
  ask people [ start-activity ]

  ;; if vaccination policy is in place, check if it should be rolled out now
  if (vaccination-policy?) [
    if (not vaccination-started? and is-vaccination-triggered?) [
      dump ">>>>>>>>>>>>>>> start of vaccination policy <<<<<<<<<<<<<<<"
      set vaccination-started? true
      roll-out-vaccination-policy
    ]
  ]

  tick
end

;; ---------------------------------------------------------------------------------------------------------------------
;; Vaccination policy procedures
;; ---------------------------------------------------------------------------------------------------------------------

to-report is-vaccination-triggered?
  report (count people with [is-sick? or is-dead?] / count people) >= VP-threshold
end

to roll-out-vaccination-policy
  ;; assuming we can vaccinate x people per tick (set in global variable people-per-tick)
  ;; we'll send vaccination appointments to all households for appropriate times, starting with next tick
  let start ticks + 1
  let people-reached 0
  let the-hospital (one-of patches with [function = "hospital"])
  ask patches with [function = "home"] [
    set people-reached people-reached + hh-size
    if (people-reached > people-per-tick) [
      set start start + 1
      set people-reached 0
    ]
    let vac-app (list start "vaccination" the-hospital)
    ask turtle-set hh-members [
      reschedule vac-app
    ]
  ]
end

;; ---------------------------------------------------------------------------------------------------------------------
;; People procedures
;; ---------------------------------------------------------------------------------------------------------------------

to reschedule [vaccination-appointment]
  ;; ignore if sick or already immune
  if (not (is-sick? or is-immune?)) [
    ;; check if we've planned something for the appointment time
    let app-time start-of vaccination-appointment
    let clashing-plan factbase:retrieve calendar [ t -> t = app-time ] ["start"]
    if (not empty? clashing-plan) [
      ;; we gotta retract it
      factbase:retract calendar (first clashing-plan)
    ]
    ;; assert the appointment
    factbase:assert calendar vaccination-appointment
  ]
end

to plan-schedule
  let next ticks + 1
  ;; everyone does their compulsory activity first
  factbase:assert calendar (list next compulsory (place-of-compulsory))
  set next next + duration-of compulsory
  ;; then picks 0-2 random activity for the day
  let n random 3
  repeat n [
    set next next + random 2 ;; randomly space out activies
    factbase:assert calendar (list next "activity" (one-of activities))
    set next next + duration-of "activity"
  ]
  ;; then heads home
  factbase:assert calendar (list next "home" my-home)
end

to check-health
  ;; are we at risk of infection at our current place?
  if (is-healthy? and any? other turtles-here with [is-infected?]) [
    ;; there is a chance that we get infected
    if (get-infected-here?) [
      turn "infected"
      update-infected-count [function] of patch-here
    ]
  ]
  ;; are we getting sick?
  if (is-infected? and is-incubation-over?) [
    turn "sick"
    ;; better go to hospital
    go-to-hospital
  ]
  ;; are we dying?
  if (is-sick? and is-sick-to-death-time-over?) [
    if (random-float 1.0 < death-rate) [
      turn "dead"
    ]
  ]
  ;; are we turning well again?
  if (is-sick? and is-sick-to-well-time-over?) [
    turn "immune"
    travel-to my-home
  ]
end

to turn [health]
  set status (position health health-status)
  set color item status health-colours
  ;; keep track in global health data base
  factbase:assert global-health (list self status ticks)
  let description (word "gets " health)
  if (health = "dead") [
    set description "dies"
  ]
  dump (word description " at " ([function] of patch-here))
end

to-report get-infected-here?
  ;; dependent on number of infected persons vs. number of persons present
  let factor (count turtles-here with [is-infected?]) / (count other turtles-here + 1) ;; add one to avoid division by 0
  let infected-prob random-float 1.0
  report infected-prob < (infection-rate * factor)
end

to-report is-incubation-over?
  ;; find when we were infected
  let result factbase:retrieve global-health [ [p s t] -> p = self and s = (position "infected" health-status) and (ticks - t) >= incubation ] ["person" "status" "timestamp"]
  if (not empty? result) [
    report true
  ]
  report false
end

to-report is-sick-to-death-time-over?
  ;; find when we got sick
  let result factbase:retrieve global-health [ [p s t] -> p = self and s = (position "sick" health-status) and (ticks - t) = sick-to-death-time ] ["person" "status" "timestamp"]
  if (not empty? result) [
    report true
  ]
  report false
end

to-report is-sick-to-well-time-over?
  ;; find when we got sick
  let result factbase:retrieve global-health [ [p s t] -> p = self and s = (position "sick" health-status) and (ticks - t) = sick-to-well-time ] ["person" "status" "timestamp"]
  if (not empty? result) [
    report true
  ]
  report false
end

to-report is-healthy?
  report status = (position "healthy" health-status)
end

to-report is-infected?
  report status = (position "infected" health-status)
end

to-report is-sick?
  report status = (position "sick" health-status)
end

to-report is-immune?
  report status = (position "immune" health-status)
end

to-report is-dead?
  report status = (position "dead" health-status)
end

to go-to-hospital
  ;; retract (rest of) schedule for today
  factbase:retract-all calendar [ t -> t >= ticks ] ["start"]
  ;; and beam yourself into a hospital bed
  travel-to one-of (patches with [function = "hospital"])
end

to start-activity
  ;; check calendar to see if we start an activity this tick
  let result factbase:retrieve calendar [ t -> t = ticks ] ["start"]
  if (not empty? result) [
    let entry (first result)
    ;; go to place of activity
    travel-to place-of entry
    ;;dump (word "is at " (activity-of entry))
    ;; if activity is vaccination, have to change health status to immune
    if (activity-of entry = "vaccination") [
      dump ("gets vaccinated")
      turn "immune"
    ]
  ]
  ;; retract the fact?
;;  factbase:retract calendar (first result)
end

to pick-activities
  set activities []
  ;; pick 1-n of the available activities as my activities to chose from when planning my schedule
  let n 1 + random (item (position "activity" place-functions) place-numbers)
  repeat n [
    let temp one-of patches with [function = "activity"]
    if (not member? temp activities) [
      set activities lput temp activities
    ]
  ]
end

to travel-to [where]
  move-to where
  ;; spread out a bit inside the patch to be visible
  set xcor xcor + first offset
  set ycor ycor + last offset
end


;; ---------------------------------------------------------------------------------------------------------------------
;; Calendar fact processing utilities
;; ---------------------------------------------------------------------------------------------------------------------

to-report start-of [fact]
  report first fact
end

to-report activity-of [fact]
  report item 1 fact
end

to-report place-of [fact]
  report last fact
end

to-report place-of-compulsory
  if (compulsory = "work") [ report my-work ]
  if (compulsory = "school") [ report my-school ]
  report my-home
end

to-report duration-of [activity]
  if (activity = "home") [
    report 1 + random (duration-of "work")
  ]
  report item (position activity place-functions) durations
end

;; ---------------------------------------------------------------------------------------------------------------------
;; General utilities
;; ---------------------------------------------------------------------------------------------------------------------

to update-infected-count [place]
  let index position place place-functions
  let value item index count-infected-at
  set count-infected-at replace-item index count-infected-at (value + 1)
end

to-report sample-empirical-distribution [probabilities values]
  ;; probabilities are not accumulated but add up to 1.0
  ;; there is a probability for each value
  let k random-float 1.0
  let i 0
  let lower-bound 0
  while [i < length probabilities] [
    ifelse (k < precision (lower-bound + item i probabilities) 4) [
      report item i values
    ] [
      set lower-bound precision (lower-bound + item i probabilities) 4
      set i i + 1
    ]
  ]
  show (word "ERROR in sample-empirical-distribution with probs " probabilities " and values " values " // k = " k " and i = " i)
  report -1
end

to dump [message]
  if (console-output?) [
    show message
  ]
end
@#$#@#$#@
GRAPHICS-WINDOW
230
10
538
319
-1
-1
50.0
1
10
1
1
1
0
1
1
1
0
5
0
5
0
0
1
ticks
30.0

SWITCH
30
302
210
335
console-output?
console-output?
0
1
-1000

BUTTON
16
17
82
50
NIL
setup
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
153
17
216
50
NIL
go
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
32
67
206
100
infection-rate
infection-rate
0
1
0.1
0.01
1
NIL
HORIZONTAL

SLIDER
31
106
206
139
num-initial-infected
num-initial-infected
0
20
2.0
1
1
NIL
HORIZONTAL

SLIDER
30
147
205
180
death-rate
death-rate
0
1
0.75
0.01
1
NIL
HORIZONTAL

PLOT
556
15
898
237
Population statistics [%]
NIL
NIL
0.0
10.0
0.0
100.0
true
true
"" ""
PENS
"healthy" 1.0 0 -10899396 true "" "plot count people with [is-healthy?] / count people * 100"
"infected" 1.0 0 -2064490 true "" "plot count people with [is-infected?] / count people * 100"
"sick" 1.0 0 -2674135 true "" "plot count people with [is-sick?] / count people * 100"
"dead" 1.0 0 -8630108 true "" "plot count people with [is-dead?] / count people * 100"
"immune" 1.0 0 -955883 true "" "plot count people with [is-immune?] / count people * 100"

MONITOR
841
246
898
291
dead
count people with [is-dead?]
0
1
11

MONITOR
557
246
615
291
healthy
count people with [is-healthy?]
0
1
11

MONITOR
626
246
689
291
infected
count people with [is-infected?]
17
1
11

MONITOR
699
246
759
291
sick
count people with [is-sick?]
17
1
11

MONITOR
769
246
832
291
immune
count people with [is-immune?]
17
1
11

SWITCH
30
208
207
241
vaccination-policy?
vaccination-policy?
0
1
-1000

SLIDER
29
246
207
279
VP-threshold
VP-threshold
0
1
0.3
0.01
1
NIL
HORIZONTAL

MONITOR
238
350
295
395
Day
ticks / ticks-per-day
0
1
11

BUTTON
86
17
149
50
step
go
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

MONITOR
567
318
624
363
home
item 0 count-infected-at
0
1
11

MONITOR
630
318
687
363
work
item 1 count-infected-at
0
1
11

MONITOR
693
318
750
363
school
item 2 count-infected-at
0
1
11

MONITOR
755
318
812
363
activity
item 3 count-infected-at
0
1
11

TEXTBOX
571
301
767
329
Number of infections happened at:
11
0.0
1

@#$#@#$#@
## WHAT IS IT?

This is a simple model of an epidemic outbreak and a policy to contain it. 

A population of agents (breed people) live in households in a small town consisting of more or less abstract locations (patches). Other than households (grey patches), these locations comprise a school (brown), work places (black), a hospital (white), and places of optional "leisure" activities like shopping, going to church or to a sports club (blue).

All children of ages 4-18 attend school, while adults go to work (unless they are unemployed or retired). Each person also picks up to two leisure activities to perform each day. 

This model mainly demonstrates the use of the factbase extension. Each person has its own calendar to schedule activities, while there is also a global factbase to keep track of everybody's changes in health status.

## HOW IT WORKS

A day consists of 8 ticks. At the beginning of each day, each person makes a schedule for the day, starting with their respective compulsory activity (work for employed adults, school for children of school age, staying at home for everyone else). They then pick 0 to 2 leisure activities to also do on that day and schedule them in. Work takes 3 ticks, school 2 and leisure activities 1 tick to perform. The rest of the day people are home. (This model does not account for weekends or holidays.)

During the day, everybody consistently checks their health status. People generally start out as healthy (represented by the colour green) but might get infected at any of the places they visit if an infected person (pink) is present at the same time. Their chance of infection depends on the disease's infection rate (slider _infection-rate_) mediated by the proportion of infected people present.

All infected people progress to becoming sick (red) after an incubation period of 7 days. Any sick person immediately seeks treatment at the hospital and abandons all other activities he or she might still have planned for the rest of the day.

Despite treatment, sick people may die (violet) after 3 days with a probability determined by the death rate of the disease (slider _death-rate_). If they are lucky enough to escape this fate, they will survive to become immune (orange) after another 2 days.

This "natural" progress of the disease may be broken by implementing a vaccination policy (switch _vaccination-policy?_). Vaccination makes both healthy and infected people immediately immune to the disease's effect. The start of the policy is determined by the proportion of sick and dead people in the total population (slider _VP-threshold_). People receive appointments for particular times to go to the hospital for vaccination and everyone accepts these and reschedules their calendar respectively, i.e. they replace the activity planned for the appointed time with the vaccination appointment. After that, they each go their merry way as before.

## HOW TO USE IT

Use the _setup_ button to initialise the model and then click _go_ to watch the epidemic unfold. You can determine the spread and deadliness of the disease by choosing different values for its infection (slider _infection-rate_) and death rate (slider _death-rate_), together with the number of people initially infected (slider _num-initial-infected_). Of course, if no-one is infected at model setup, there will be no epidemic outbreak!

The plot shows time series of the proportions of the population who are of a particular  health status: healthy (green), pink (infected), red (sick), immune (orange) or dead (violet), whereas the monitors underneath it display the absolute numbers.


## THINGS TO TRY

Play around a bit with the parameters that influence the disease without having vaccination in place (switch _vaccination-policy?_ turned off). Then see what difference it makes turning it on. The slider _VP-threshold_ determines when the vaccination policy is rolled out; e.g. if it's set to 0.3, 30% of the population has to be sick or dead before vaccination starts.

Can you find a combination of model parameters where not everyone gets infected, i.e. where there is still at least one healthy person at the end of the simulation after the epidemic has run its course?


## NETLOGO FEATURES

This model uses the factbase extension. See http://cfpm.org/discussionpapers/154/factbase-a-netlogo-extension or https://github.com/ruthmore/netlogo-factbase if you are interested in the source code.

## RELATED MODELS

There are a lot of epidemic models out there, often called SIR models for dividing the population into Susceptible / Infected / Recovered (with immunity) groups. Not all of them are of the agent-based variety, of course, but a search on the mighty web should provide you with other examples if you feel so inclined. 

The main purpose of this particular model is to demonstrate the use of the factbase extension.

## CREDITS AND REFERENCES
@#$#@#$#@
default
true
0
Polygon -7500403 true true 150 5 40 250 150 205 260 250

airplane
true
0
Polygon -7500403 true true 150 0 135 15 120 60 120 105 15 165 15 195 120 180 135 240 105 270 120 285 150 270 180 285 210 270 165 240 180 180 285 195 285 165 180 105 180 60 165 15

arrow
true
0
Polygon -7500403 true true 150 0 0 150 105 150 105 293 195 293 195 150 300 150

box
false
0
Polygon -7500403 true true 150 285 285 225 285 75 150 135
Polygon -7500403 true true 150 135 15 75 150 15 285 75
Polygon -7500403 true true 15 75 15 225 150 285 150 135
Line -16777216 false 150 285 150 135
Line -16777216 false 150 135 15 75
Line -16777216 false 150 135 285 75

bug
true
0
Circle -7500403 true true 96 182 108
Circle -7500403 true true 110 127 80
Circle -7500403 true true 110 75 80
Line -7500403 true 150 100 80 30
Line -7500403 true 150 100 220 30

butterfly
true
0
Polygon -7500403 true true 150 165 209 199 225 225 225 255 195 270 165 255 150 240
Polygon -7500403 true true 150 165 89 198 75 225 75 255 105 270 135 255 150 240
Polygon -7500403 true true 139 148 100 105 55 90 25 90 10 105 10 135 25 180 40 195 85 194 139 163
Polygon -7500403 true true 162 150 200 105 245 90 275 90 290 105 290 135 275 180 260 195 215 195 162 165
Polygon -16777216 true false 150 255 135 225 120 150 135 120 150 105 165 120 180 150 165 225
Circle -16777216 true false 135 90 30
Line -16777216 false 150 105 195 60
Line -16777216 false 150 105 105 60

car
false
0
Polygon -7500403 true true 300 180 279 164 261 144 240 135 226 132 213 106 203 84 185 63 159 50 135 50 75 60 0 150 0 165 0 225 300 225 300 180
Circle -16777216 true false 180 180 90
Circle -16777216 true false 30 180 90
Polygon -16777216 true false 162 80 132 78 134 135 209 135 194 105 189 96 180 89
Circle -7500403 true true 47 195 58
Circle -7500403 true true 195 195 58

circle
false
0
Circle -7500403 true true 0 0 300

circle 2
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240

cow
false
0
Polygon -7500403 true true 200 193 197 249 179 249 177 196 166 187 140 189 93 191 78 179 72 211 49 209 48 181 37 149 25 120 25 89 45 72 103 84 179 75 198 76 252 64 272 81 293 103 285 121 255 121 242 118 224 167
Polygon -7500403 true true 73 210 86 251 62 249 48 208
Polygon -7500403 true true 25 114 16 195 9 204 23 213 25 200 39 123

cylinder
false
0
Circle -7500403 true true 0 0 300

dot
false
0
Circle -7500403 true true 90 90 120

face happy
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 255 90 239 62 213 47 191 67 179 90 203 109 218 150 225 192 218 210 203 227 181 251 194 236 217 212 240

face neutral
false
0
Circle -7500403 true true 8 7 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Rectangle -16777216 true false 60 195 240 225

face sad
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 168 90 184 62 210 47 232 67 244 90 220 109 205 150 198 192 205 210 220 227 242 251 229 236 206 212 183

fish
false
0
Polygon -1 true false 44 131 21 87 15 86 0 120 15 150 0 180 13 214 20 212 45 166
Polygon -1 true false 135 195 119 235 95 218 76 210 46 204 60 165
Polygon -1 true false 75 45 83 77 71 103 86 114 166 78 135 60
Polygon -7500403 true true 30 136 151 77 226 81 280 119 292 146 292 160 287 170 270 195 195 210 151 212 30 166
Circle -16777216 true false 215 106 30

flag
false
0
Rectangle -7500403 true true 60 15 75 300
Polygon -7500403 true true 90 150 270 90 90 30
Line -7500403 true 75 135 90 135
Line -7500403 true 75 45 90 45

flower
false
0
Polygon -10899396 true false 135 120 165 165 180 210 180 240 150 300 165 300 195 240 195 195 165 135
Circle -7500403 true true 85 132 38
Circle -7500403 true true 130 147 38
Circle -7500403 true true 192 85 38
Circle -7500403 true true 85 40 38
Circle -7500403 true true 177 40 38
Circle -7500403 true true 177 132 38
Circle -7500403 true true 70 85 38
Circle -7500403 true true 130 25 38
Circle -7500403 true true 96 51 108
Circle -16777216 true false 113 68 74
Polygon -10899396 true false 189 233 219 188 249 173 279 188 234 218
Polygon -10899396 true false 180 255 150 210 105 210 75 240 135 240

house
false
0
Rectangle -7500403 true true 45 120 255 285
Rectangle -16777216 true false 120 210 180 285
Polygon -7500403 true true 15 120 150 15 285 120
Line -16777216 false 30 120 270 120

leaf
false
0
Polygon -7500403 true true 150 210 135 195 120 210 60 210 30 195 60 180 60 165 15 135 30 120 15 105 40 104 45 90 60 90 90 105 105 120 120 120 105 60 120 60 135 30 150 15 165 30 180 60 195 60 180 120 195 120 210 105 240 90 255 90 263 104 285 105 270 120 285 135 240 165 240 180 270 195 240 210 180 210 165 195
Polygon -7500403 true true 135 195 135 240 120 255 105 255 105 285 135 285 165 240 165 195

line
true
0
Line -7500403 true 150 0 150 300

line half
true
0
Line -7500403 true 150 0 150 150

pentagon
false
0
Polygon -7500403 true true 150 15 15 120 60 285 240 285 285 120

person
false
0
Circle -7500403 true true 110 5 80
Polygon -7500403 true true 105 90 120 195 90 285 105 300 135 300 150 225 165 300 195 300 210 285 180 195 195 90
Rectangle -7500403 true true 127 79 172 94
Polygon -7500403 true true 195 90 240 150 225 180 165 105
Polygon -7500403 true true 105 90 60 150 75 180 135 105

plant
false
0
Rectangle -7500403 true true 135 90 165 300
Polygon -7500403 true true 135 255 90 210 45 195 75 255 135 285
Polygon -7500403 true true 165 255 210 210 255 195 225 255 165 285
Polygon -7500403 true true 135 180 90 135 45 120 75 180 135 210
Polygon -7500403 true true 165 180 165 210 225 180 255 120 210 135
Polygon -7500403 true true 135 105 90 60 45 45 75 105 135 135
Polygon -7500403 true true 165 105 165 135 225 105 255 45 210 60
Polygon -7500403 true true 135 90 120 45 150 15 180 45 165 90

sheep
false
15
Circle -1 true true 203 65 88
Circle -1 true true 70 65 162
Circle -1 true true 150 105 120
Polygon -7500403 true false 218 120 240 165 255 165 278 120
Circle -7500403 true false 214 72 67
Rectangle -1 true true 164 223 179 298
Polygon -1 true true 45 285 30 285 30 240 15 195 45 210
Circle -1 true true 3 83 150
Rectangle -1 true true 65 221 80 296
Polygon -1 true true 195 285 210 285 210 240 240 210 195 210
Polygon -7500403 true false 276 85 285 105 302 99 294 83
Polygon -7500403 true false 219 85 210 105 193 99 201 83

square
false
0
Rectangle -7500403 true true 30 30 270 270

square 2
false
0
Rectangle -7500403 true true 30 30 270 270
Rectangle -16777216 true false 60 60 240 240

star
false
0
Polygon -7500403 true true 151 1 185 108 298 108 207 175 242 282 151 216 59 282 94 175 3 108 116 108

target
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240
Circle -7500403 true true 60 60 180
Circle -16777216 true false 90 90 120
Circle -7500403 true true 120 120 60

tree
false
0
Circle -7500403 true true 118 3 94
Rectangle -6459832 true false 120 195 180 300
Circle -7500403 true true 65 21 108
Circle -7500403 true true 116 41 127
Circle -7500403 true true 45 90 120
Circle -7500403 true true 104 74 152

triangle
false
0
Polygon -7500403 true true 150 30 15 255 285 255

triangle 2
false
0
Polygon -7500403 true true 150 30 15 255 285 255
Polygon -16777216 true false 151 99 225 223 75 224

truck
false
0
Rectangle -7500403 true true 4 45 195 187
Polygon -7500403 true true 296 193 296 150 259 134 244 104 208 104 207 194
Rectangle -1 true false 195 60 195 105
Polygon -16777216 true false 238 112 252 141 219 141 218 112
Circle -16777216 true false 234 174 42
Rectangle -7500403 true true 181 185 214 194
Circle -16777216 true false 144 174 42
Circle -16777216 true false 24 174 42
Circle -7500403 false true 24 174 42
Circle -7500403 false true 144 174 42
Circle -7500403 false true 234 174 42

turtle
true
0
Polygon -10899396 true false 215 204 240 233 246 254 228 266 215 252 193 210
Polygon -10899396 true false 195 90 225 75 245 75 260 89 269 108 261 124 240 105 225 105 210 105
Polygon -10899396 true false 105 90 75 75 55 75 40 89 31 108 39 124 60 105 75 105 90 105
Polygon -10899396 true false 132 85 134 64 107 51 108 17 150 2 192 18 192 52 169 65 172 87
Polygon -10899396 true false 85 204 60 233 54 254 72 266 85 252 107 210
Polygon -7500403 true true 119 75 179 75 209 101 224 135 220 225 175 261 128 261 81 224 74 135 88 99

wheel
false
0
Circle -7500403 true true 3 3 294
Circle -16777216 true false 30 30 240
Line -7500403 true 150 285 150 15
Line -7500403 true 15 150 285 150
Circle -7500403 true true 120 120 60
Line -7500403 true 216 40 79 269
Line -7500403 true 40 84 269 221
Line -7500403 true 40 216 269 79
Line -7500403 true 84 40 221 269

wolf
false
0
Polygon -16777216 true false 253 133 245 131 245 133
Polygon -7500403 true true 2 194 13 197 30 191 38 193 38 205 20 226 20 257 27 265 38 266 40 260 31 253 31 230 60 206 68 198 75 209 66 228 65 243 82 261 84 268 100 267 103 261 77 239 79 231 100 207 98 196 119 201 143 202 160 195 166 210 172 213 173 238 167 251 160 248 154 265 169 264 178 247 186 240 198 260 200 271 217 271 219 262 207 258 195 230 192 198 210 184 227 164 242 144 259 145 284 151 277 141 293 140 299 134 297 127 273 119 270 105
Polygon -7500403 true true -1 195 14 180 36 166 40 153 53 140 82 131 134 133 159 126 188 115 227 108 236 102 238 98 268 86 269 92 281 87 269 103 269 113

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270
@#$#@#$#@
NetLogo 6.1.1
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
default
0.0
-0.2 0 0.0 1.0
0.0 1 1.0 0.0
0.2 0 0.0 1.0
link direction
true
0
Line -7500403 true 150 150 90 180
Line -7500403 true 150 150 210 180
@#$#@#$#@
0
@#$#@#$#@
