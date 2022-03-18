#!/usr/bin/env groovy
def call(val) {
    try {
        return val == null || val == '';
    } catch(Exception ex) {}
    return true;
}
