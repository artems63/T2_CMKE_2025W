% environmental_score.pl

% Base eco score per mode (0..1)
base_score(walk, 1.00).
base_score(bike, 0.95).
base_score(e_bike, 0.93).
base_score(bikeshare, 0.95).
base_score(public_transport, 0.80).
base_score(electric_car, 0.60).
base_score(gas_car, 0.25).

% Penalty for bad weather for active modes (still eco-friendly, but less feasible)
weather_penalty(true, 0.0).   % weather_ok = true
weather_penalty(false, 0.15). % weather_ok = false -> reduce score

% Distance penalty (very mild, keeps it simple and stable)
distance_penalty(DistanceMeters, Penalty) :-
    DistanceMeters =< 1000, Penalty is 0.00;
    DistanceMeters =< 5000, Penalty is 0.05;
    DistanceMeters =< 10000, Penalty is 0.10;
    Penalty is 0.15.

% Environmental factor as a general "how eco-favorable is it to choose eco modes now"
environmental_factor(DistanceMeters, WeatherOk, WantsSustainable, Factor) :-
    distance_penalty(DistanceMeters, DP),
    weather_penalty(WeatherOk, WP),
    (WantsSustainable = true -> SP is 0.05 ; SP is 0.00),
    Raw is 1.0 - DP - WP + SP,
    clamp01(Raw, Factor).

clamp01(X, 0.0) :- X < 0.0, !.
clamp01(X, 1.0) :- X > 1.0, !.
clamp01(X, X).
