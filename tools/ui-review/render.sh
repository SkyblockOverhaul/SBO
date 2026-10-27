#!/bin/sh
# Offline only. Never launches Minecraft or connects to an SBO service.
set -eu
version="${1:-26.2}"
screen="${2:-settings}"
case "$version" in 26.1.2|26.2|26.3) ;; *) echo "Usage: $0 [26.1.2|26.2|26.3] [settings|diana|events|achievements|partyfinder|pf-forms|pf-rest|category]" >&2; exit 2 ;; esac
case "$screen" in
    settings) suite=net.sbo.mod.test.SettingsPresentationTest; export SBO_SETTINGS_REVIEW=1; folder=settings-general ;;
    diana) suite=net.sbo.mod.test.DianaPresentationTest; export SBO_DIANA_REVIEW=1; folder=settings-diana ;;
    partyfinder) suite=net.sbo.mod.test.PartyFinderPresentationTest; export SBO_PF_REVIEW=1; folder=partyfinder-list ;;
    pf-create) suite=net.sbo.mod.test.PartyFinderFormsPresentationTest; export SBO_PF_FORMS_REVIEW=create; folder=partyfinder-forms ;;
    pf-filters) suite=net.sbo.mod.test.PartyFinderFormsPresentationTest; export SBO_PF_FORMS_REVIEW=filters; folder=partyfinder-forms ;;
    pf-forms) suite=net.sbo.mod.test.PartyFinderFormsPresentationTest; export SBO_PF_FORMS_REVIEW=all; folder=partyfinder-forms ;;
    pf-rest) suite=net.sbo.mod.test.PartyFinderRestPresentationTest; export SBO_PF_REST_REVIEW=all; folder=partyfinder-rest ;;
    events) suite=net.sbo.mod.test.EventsPresentationTest; export SBO_EVENTS_REVIEW=1; folder=events ;;
    achievements) suite=net.sbo.mod.test.AchievementsPresentationTest; export SBO_ACH_REVIEW=1; folder=achievements ;;
    sounds) suite=net.sbo.mod.test.SoundsPresentationTest; export SBO_SOUNDS_REVIEW=1; folder=sounds ;;
    cloud) suite=net.sbo.mod.test.CloudPresentationTest; export SBO_CLOUD_REVIEW=1; folder=cloud ;;
    badge) suite=net.sbo.mod.test.BadgePresentationTest; export SBO_BADGE_REVIEW=1; folder=badge ;;
    overlay) suite=net.sbo.mod.test.OverlayPresentationTest; export SBO_OVERLAY_REVIEW=1; folder=overlay-editor ;;
    Medal|PartyCommands|Customization|Themes|QOL|Debug) suite=net.sbo.mod.test.SettingsSweepTest; export SBO_SETTINGS_SWEEP=1; export SBO_SETTINGS_CATEGORY="$screen"; folder=settings-sweep ;;
    *) echo "Unknown review screen: $screen" >&2; exit 2 ;;
esac
cd "$(dirname "$0")/../.."
if [ -z "${JAVA_HOME:-}" ]; then
    for cached_jdk in "$HOME"/.gradle/jdks/*/*/Contents/Home; do
        if [ -x "$cached_jdk/bin/java" ]; then export JAVA_HOME="$cached_jdk"; break; fi
    done
fi
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Djava.awt.headless=true" \
./gradlew ":${version}-fabric:test" --rerun \
    --tests "$suite" --offline --no-daemon --console=plain
case "$screen" in
    events|achievements|sounds|cloud|badge|overlay) echo "Review images: build/ui-review/${folder}/" ;;
    *) echo "Review images: build/ui-review/${folder}/${version}-fabric/" ;;
esac
