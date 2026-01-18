% environmental_score.pl

% Represents environmental friendliness per km

base_score(walk, 1.00).
base_score(bike, 0.95).
base_score(e_bike, 0.93).
base_score(bikeshare, 0.95).
base_score(public_transport, 0.80).
base_score(electric_car, 0.60).
base_score(gas_car, 0.25).
base_score(scooter, 0.90).
base_score(e_scooter, 0.88).

distance_penalty(DistanceMeters, Penalty) :-
    DistanceMeters =< 0, !,
    Penalty is 0.0.

distance_penalty(DistanceMeters, Penalty) :-
    Km is DistanceMeters / 1000.0,
    Raw is log(1 + Km) / log(1 + 15),
    Penalty is min(0.25, Raw).

mode_weather_penalty(_Mode, true, 0.0).

mode_weather_penalty(Mode, false, Penalty) :-
    member(Mode, [walk, bike, e_bike, bikeshare, scooter, e_scooter]),
    Penalty is 0.15.

mode_weather_penalty(Mode, false, 0.0) :-
    member(Mode, [public_transport, electric_car, gas_car]).

environmental_factor(DistanceMeters, WeatherOk, WantsSustainable, Factor) :-
    distance_penalty(DistanceMeters, DP),

    % Weather impact by distance
    ( WeatherOk = true
      -> WP is 0.0
      ;  WP is DP * 0.6
    ),

    % Sustainability preference is strongest for short & medium trips
    ( WantsSustainable = true
      -> SP is max(0.0, 0.12 - DP)
      ;  SP is 0.0
    ),

    Raw is 1.0 - DP - WP + SP,
    clamp01(Raw, Factor).

mode_environmental_factor(Mode, DistanceMeters, WeatherOk, Factor) :-
    base_score(Mode, Base),
    distance_penalty(DistanceMeters, DP),
    mode_weather_penalty(Mode, WeatherOk, WP),
    Raw is Base - DP - WP,
    clamp01(Raw, Factor).

clamp01(X, 0.0) :- X < 0.0, !.
clamp01(X, 1.0) :- X > 1.0, !.
clamp01(X, X).
