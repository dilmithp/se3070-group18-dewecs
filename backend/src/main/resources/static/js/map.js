/*
 * Google Maps for DEWECS pages. A page opts in with the mapPicker / mapView fragments (templates/fragments/layout.html),
 * which carry the key and the ids of the latitude and longitude inputs in data attributes. Without them nothing runs.
 *   [data-map-picker]  click the map or drag the marker; fills the two inputs
 *   [data-map-view]    shows one marker (data-lat, data-lng)
 */
(function () {
    var pickers = document.querySelectorAll('[data-map-picker], [data-map-view]');
    if (!pickers.length) {
        return;
    }
    var SRI_LANKA = { lat: 7.8731, lng: 80.7718 };

    function init() {
        pickers.forEach(function (box) {
            var canvas = box.querySelector('.map-canvas');
            var view = box.hasAttribute('data-map-view');
            var latInput = view ? null : document.getElementById(box.getAttribute('data-lat-input'));
            var lngInput = view ? null : document.getElementById(box.getAttribute('data-lng-input'));
            var lat = parseFloat(view ? box.getAttribute('data-lat') : latInput && latInput.value);
            var lng = parseFloat(view ? box.getAttribute('data-lng') : lngInput && lngInput.value);
            var has = !isNaN(lat) && !isNaN(lng);
            var map = new google.maps.Map(canvas, {
                center: has ? { lat: lat, lng: lng } : SRI_LANKA,
                zoom: has ? 13 : 7,
                streetViewControl: false,
                mapTypeControl: false
            });
            var marker = new google.maps.Marker({ map: map, position: has ? { lat: lat, lng: lng } : null, draggable: !view });
            if (view) {
                return;
            }
            function place(position) {
                marker.setPosition(position);
                latInput.value = position.lat().toFixed(6);
                lngInput.value = position.lng().toFixed(6);
                latInput.dispatchEvent(new Event('change', { bubbles: true }));
            }
            map.addListener('click', function (e) { place(e.latLng); });
            marker.addListener('dragend', function (e) { place(e.latLng); });
            // typing the coordinates moves the marker
            [latInput, lngInput].forEach(function (input) {
                input.addEventListener('change', function () {
                    var a = parseFloat(latInput.value);
                    var b = parseFloat(lngInput.value);
                    if (!isNaN(a) && !isNaN(b)) {
                        marker.setPosition({ lat: a, lng: b });
                        map.panTo({ lat: a, lng: b });
                    }
                });
            });
            var here = box.querySelector('[data-use-my-location]');
            if (here) {
                if (!navigator.geolocation) {
                    here.hidden = true;
                }
                here.addEventListener('click', function () {
                    navigator.geolocation.getCurrentPosition(function (p) {
                        var position = new google.maps.LatLng(p.coords.latitude, p.coords.longitude);
                        place(position);
                        map.setZoom(14);
                        map.panTo(position);
                    });
                });
            }
        });
    }

    window.dewecsMapReady = init;
    var key = pickers[0].getAttribute('data-key');
    var script = document.createElement('script');
    script.src = 'https://maps.googleapis.com/maps/api/js?key=' + encodeURIComponent(key) + '&callback=dewecsMapReady';
    script.async = true;
    script.onerror = function () {
        pickers.forEach(function (box) { box.hidden = true; });
    };
    document.head.appendChild(script);
})();
