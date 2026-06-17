# Incentive Scheme calculations

There are two free parameters for the Incentive Scheme, with values that are dependent on government policy:
- the qualifying threshold, which is the proportion of the total required time that needs to be worked in order to
  qualify.
- the discount award, which is the rate at which discounted time is accrued once a person has qualified.

These are configurable within the service through the `IncentiveSchemeConfiguration`, but can largely be considered to
be fixed, as these are likely to be changed rarely if at all. The following Spring configuration values are used to set
these:
- `incentive-scheme.qualifying-time-threshold`: This value is expressed as a proportion of the total requirement time
  between 0.0 (0%) and 1.0 (100%). The current value is **0.25** (25%).
- `incentive-scheme.discount-awarded-for-qualifying-time`: This is expressed as a proportion of the worked time between
  0.0 (no discount awarded) and 1.0 (a 1:1 award, i.e. 1 hour is discounted for every hour worked). The current value is
  **0.5** (i.e. 30 minutes for every hour worked).

Together, these two values control most of the mathematical behaviour of the Incentive Scheme.

## Maximum discount

The maximum possible discount is the point at which enough discounted time has been earned that the amount of time
worked has reached the discounted requirement. This value is automatically calculated by the service. With the current
configuration, this value is 25%.

The chart below represents how time worked against the requirement (**A**) affects the total required time (**B**)
as a result of accruing discounted time (**C**).
Note how no discount is accrued before the qualifying time threshold (**1**).
The maximum discount ratio is reached at the time when **A** and **B** intersect (**2**).

```
     |                             [A]
100% |_________:. . . . . . : . . / .
     |         :\____       :   / :
     |         :     \____  : /   :
     |         :          \_*__   :
     |         :          / :  \____
     |         :        /   :     : \
     |         :      /     :     :  [B]
     |         :    /       :     :
     |         :  /         :     :
     |         :/           :     :
     |        /:            :     :
     |      /  :            :     :__[C]
     |    /    :            :____/:
     |  /      :       ____/:     :
     |/        : ____/      :     :
  0% +---------:------------:-----:--
     0%       [1]          [2]   100%
       Time worked for requirement
```

Line **A** has a slope of 1.
Lines **B** and **C** have a slope of 0 before (**1**) and `-discount` and `+discount` after that point
respectively.

The time (**2**) can be calculated as:

```
1 + dq
------
1 + d
```

where `d` is the discount ratio and `q` is the qualifying threshold.
For the default values of `d` = 0.5 and `q` = 0.25, this works out to 0.75.

The maximum discount ratio is just 1 minus this value, in this case 0.25, or 25%.

## Progress in the scheme

There are two discount figures that are tracked within the Incentive Scheme: the **current discount** and the
**projected discount**. These are, respectively, the reduction in time that a person has already gained by taking part,
and the total reduction in time that a person can expect by continuing to participate in the scheme.

For the current discount:
- if someone is ineligible or has not reached the qualifying threshold for their requirement, the current discount is 0.
- otherwise, it is the amount of qualifying time they have worked *above* the threshold, multiplied by the discount
  award.

For the projected discount:
- if someone is ineligible, the projected discount is 0.
- if someone has been disqualified, it is equal to the current discount, as they are no longer able to gain further
  discounts.
- otherwise, it is equal to the maximum possible discount for their requirement.

For example, if someone has a requirement to perform 100 hours of unpaid work:
- their qualifying threshold is 0.25 * 100 = 25 hours.
- their maximum possible discount is 0.25 * 100 = 25 hours.

If they are eligible for the Incentive Scheme and have undertaken 36 hours of qualifying work, then:
- they have worked for 11 hours above the qualifying threshold, and have earned a current discount of 0.5 * 11 = 5 hours
  and 30 minutes.
- their projected discount is the maximum possible discount, 25 hours.

If they then have an unacceptable absence and are disqualified, then:
- their current discount remains at 5 hours and 30 minutes.
- their projected discount is now also 5 hours and 30 minutes.
