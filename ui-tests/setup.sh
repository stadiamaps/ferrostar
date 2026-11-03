#!/usr/bin/env bash
# Called by the Trailblaze action after the device boots, before the app is installed.
set -euo pipefail

if [[ "${1:-}" == android ]]; then
	# Fresh emulators can run in their boot locale without persisting it.
	# Trailblaze otherwise restarts the framework and checks its locale before it is ready again.
	if [[ "$(adb shell getprop ro.kernel.qemu)" == 1 &&
	-z "$(adb shell getprop persist.sys.locale)" ]]; then
		boot_locale="$(adb shell getprop ro.product.locale)"
		if [[ -z "$boot_locale" ]]; then
			echo "Android emulator does not declare a boot locale" >&2
			exit 1
		fi
		adb shell su 0 setprop persist.sys.locale "$boot_locale"
	fi
	adb reverse tcp:8765 tcp:8765
fi

node ui-tests/server.mjs >"${RUNNER_TEMP:-/tmp}/ferrostar-ui-server.log" 2>&1 &
server_pid=$!
for ((attempt = 0; attempt < 30; attempt++)); do
	# Check our process as well: an unrelated server on this port must not count as ready.
	if ! kill -0 "$server_pid" 2>/dev/null; then
		cat "${RUNNER_TEMP:-/tmp}/ferrostar-ui-server.log"
		exit 1
	fi
	if [[ "$(curl --fail --silent --max-time 2 http://127.0.0.1:8765/health)" == "ferrostar-ui:$server_pid" ]]; then
		exit 0
	fi
	sleep 1
done
echo "Fixture server did not become ready" >&2
kill "$server_pid"
exit 1
