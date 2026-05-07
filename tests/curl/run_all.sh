#!/usr/bin/env bash
# End-to-end smoke: seed → split flow → friends.
# Sunucu http://localhost:8080'da ayakta olmalı.
#
# Kullanım:
#   ./tests/curl/run_all.sh
#   BASE=http://staging.example.com ./tests/curl/run_all.sh

set -eu
cd "$(dirname "$0")"

# Sunucu erişilebilir mi?
BASE="${BASE:-http://localhost:8080}"
if ! curl -s -o /dev/null --max-time 3 "$BASE/health"; then
    echo "✗ $BASE/health erişilemiyor — sunucu ayakta mı? (./gradlew run)"
    exit 1
fi

./seed.sh
./01_split_payment.sh
./02_friends.sh

echo
echo "✓ Tüm smoke testler tamamlandı."
