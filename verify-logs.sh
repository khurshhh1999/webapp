#!/bin/bash
echo "Verifying log setup..."

# Check directories
for dir in "/var/log/myapp" "/opt/myapp"; do
    if [ ! -d "$dir" ]; then
        echo "ERROR: Directory $dir does not exist"
        exit 1
    fi
    echo "Directory $dir exists with permissions:"
    ls -ld "$dir"
done

# Check log files
for file in "/var/log/myapp/application.log" "/var/log/myapp/myapp.log"; do
    if [ ! -f "$file" ]; then
        echo "ERROR: Log file $file does not exist"
        exit 1
    fi
    echo "Log file $file exists with permissions:"
    ls -l "$file"
done

# Verify CloudWatch agent
if [ ! -f "/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json" ]; then
    echo "ERROR: CloudWatch agent config not found"
    exit 1
fi

# Check CloudWatch agent service
if ! systemctl is-enabled amazon-cloudwatch-agent >/dev/null 2>&1; then
    echo "ERROR: CloudWatch agent service not enabled"
    exit 1
fi

echo "All verifications passed!"
exit 0