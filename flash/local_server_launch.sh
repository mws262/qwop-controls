#!/bin/bash
# Run the policy server on port 843. Flash will request crossdomain.xml through this before allowing XMLSocket communication.
sudo python flashpolicyd.py -f crossdomain.xml &
# Start the http server. Can access through localhost:8000.
python3 -m http.server
