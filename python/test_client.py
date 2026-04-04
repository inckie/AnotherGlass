#!/usr/bin/env python3
"""
Comprehensive test suite for the media client
"""

import json
from client import MediaDebugController, PlaybackStateValue

def test_initial_state():
    """Test 1: Initial State"""
    print("Test 1: Initial State")
    media = MediaDebugController()
    print(f"  media_running: {media.media_running}")
    print(f"  current_track: {media.current_track}")
    print(f"  playback_state: {media.playback_state}")
    assert media.media_running == False
    assert media.current_track is None
    assert media.playback_state == PlaybackStateValue.NoneState
    print("  ✓ PASSED\n")

def test_start_app():
    """Test 2: Start App"""
    print("Test 2: Start App")
    media = MediaDebugController()
    media.start_app()
    print(f"  media_running: {media.media_running}")
    print(f"  current_track_index: {media.current_track_index}")
    print(f"  current_track: {media.current_track}")
    assert media.media_running == True
    assert media.current_track_index == 0
    assert media.current_track is not None
    print("  ✓ PASSED\n")

def test_available_buttons():
    """Test 3: Check available buttons"""
    print("Test 3: Available Buttons")
    media = MediaDebugController()
    media.start_app()
    buttons = media.available_buttons
    for button, enabled in buttons.items():
        print(f"  {button}: {enabled}")
    assert buttons["start_app"] == False
    assert buttons["stop_app"] == True
    assert buttons["play"] == True
    assert buttons["pause"] == False
    assert buttons["prev"] == True
    assert buttons["next"] == True
    print("  ✓ PASSED\n")

def test_play():
    """Test 4: Play"""
    print("Test 4: Play")
    media = MediaDebugController()
    media.start_app()
    media.play()
    print(f"  playback_state: {media.playback_state}")
    assert media.playback_state == PlaybackStateValue.Playing
    print("  ✓ PASSED\n")

def test_to_media_state():
    """Test 5: to_media_state()"""
    print("Test 5: to_media_state()")
    media = MediaDebugController()
    media.start_app()
    media.play()
    state = media.to_media_state()
    json_str = json.dumps(state, default=str, indent=2)
    print(json_str[:300] + "..." if len(json_str) > 300 else json_str)
    assert state is not None
    assert "title" in state
    assert "playbackState" in state
    assert "artwork" in state
    print("  ✓ PASSED\n")

def test_next_track():
    """Test 6: Next Track"""
    print("Test 6: Next Track")
    media = MediaDebugController()
    media.start_app()
    first_track_title = media.current_track["title"]
    media.next_track()
    second_track_title = media.current_track["title"]
    print(f"  First track: {first_track_title}")
    print(f"  Second track: {second_track_title}")
    print(f"  current_track_index: {media.current_track_index}")
    assert first_track_title != second_track_title
    assert media.current_track_index == 1
    print("  ✓ PASSED\n")

def test_prev_track():
    """Test 7: Previous Track"""
    print("Test 7: Previous Track")
    media = MediaDebugController()
    media.start_app()
    media.next_track()
    second_track_title = media.current_track["title"]
    media.prev_track()
    first_track_title = media.current_track["title"]
    print(f"  From track: {second_track_title}")
    print(f"  To track: {first_track_title}")
    print(f"  current_track_index: {media.current_track_index}")
    assert second_track_title != first_track_title
    assert media.current_track_index == 0
    print("  ✓ PASSED\n")

def test_pause():
    """Test 8: Pause"""
    print("Test 8: Pause")
    media = MediaDebugController()
    media.start_app()
    media.play()
    media.pause()
    print(f"  playback_state: {media.playback_state}")
    assert media.playback_state == PlaybackStateValue.Paused
    print("  ✓ PASSED\n")

def test_stop_app():
    """Test 9: Stop App"""
    print("Test 9: Stop App")
    media = MediaDebugController()
    media.start_app()
    media.stop_app()
    print(f"  media_running: {media.media_running}")
    print(f"  current_track: {media.current_track}")
    print(f"  playback_state: {media.playback_state}")
    assert media.media_running == False
    assert media.current_track is None
    assert media.playback_state == PlaybackStateValue.NoneState
    print("  ✓ PASSED\n")

def test_artwork_parsing():
    """Test 10: Artwork Parsing"""
    print("Test 10: Artwork Parsing")
    media = MediaDebugController()
    media.start_app()
    track = media.current_track
    artwork = track.get("artwork")
    print(f"  Track: {track['title']}")
    print(f"  Has artwork: {artwork is not None}")
    print(f"  Artwork starts with 'data:image/jpeg;base64,': {artwork.startswith('data:image/jpeg;base64,') if artwork else False}")
    assert artwork is not None
    assert artwork.startswith("data:image/jpeg;base64,")
    print("  ✓ PASSED\n")

if __name__ == "__main__":
    print("=" * 60)
    print("MEDIA CLIENT TEST SUITE")
    print("=" * 60 + "\n")

    try:
        test_initial_state()
        test_start_app()
        test_available_buttons()
        test_play()
        test_to_media_state()
        test_next_track()
        test_prev_track()
        test_pause()
        test_stop_app()
        test_artwork_parsing()

        print("=" * 60)
        print("✓ ALL TESTS PASSED!")
        print("=" * 60)
    except Exception as e:
        print(f"\n❌ TEST FAILED: {e}")
        import traceback
        traceback.print_exc()
        exit(1)


