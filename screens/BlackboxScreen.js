import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  SafeAreaView,
  Vibration,
  Animated,
  Easing,
  ScrollView,
} from "react-native";
import Icon from "react-native-vector-icons/MaterialIcons";
// import { LinearGradient } from "expo-linear-gradient";
import { useNavigation } from "@react-navigation/native";

const BlackboxScreen = () => {
  const [dangerMode, setDangerMode] = useState(false);
  const [waveAnim] = useState(new Animated.Value(0));
  const [isEngineStopped, setIsEngineStopped] = useState(false); // New state for engine status
  const navigation = useNavigation();

  const startDangerMode = () => {
    setDangerMode(true);
    Vibration.vibrate([0, 500, 500], true);

    // Create looping ripple effect with easing for smooth animation
    Animated.loop(
      Animated.sequence([
        Animated.timing(waveAnim, {
          toValue: 1,
          duration: 2000, // Duration for a slow, fluid ripple
          easing: Easing.inOut(Easing.ease), // Easing for smooth flow
          useNativeDriver: true,
        }),
        Animated.timing(waveAnim, {
          toValue: 0,
          duration: 2000, // Consistent duration
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ])
    ).start();
  };

  const stopDangerMode = () => {
    setDangerMode(false);
    Vibration.cancel();
    waveAnim.stopAnimation();
  };

  const handleEngineToggle = () => {
    if (isEngineStopped) {
      // If the engine is currently stopped, start the engine
      setIsEngineStopped(false);
      stopDangerMode();
    } else {
      // If the engine is running, stop it
      setIsEngineStopped(true);
      startDangerMode();
      // Stop engine and vibrate for 10 seconds
      setTimeout(() => {
        stopDangerMode();
      }, 10000); // 10 seconds
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          style={styles.backButton}
        >
          <Icon name="arrow-back" size={25} color="white" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Blackbox</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* Subscription Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Subscription Type</Text>
          <Text style={styles.sectionContent}>
            Coverage 
            {/* <Ionicons name="checkmark" size={16} color="#EABB63" /> */}
          </Text>
          <Text style={styles.sectionContent}>No Coverage</Text>
        </View>

        {/* Devices Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>My Devices</Text>
          <Text style={styles.sectionContent}>
            Status: Active 
            {/* <Ionicons name="checkmark" size={16} color="#EABB63" /> */}
          </Text>
          <Text style={styles.sectionContent}>ID: XX546</Text>
        </View>

        {/* Vehicle Info Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Vehicle Info</Text>
          <Text style={styles.sectionContent}>Manufacturer: Toyota</Text>
          <Text style={styles.sectionContent}>Model: Rx 2004</Text>
        </View>

        {/* Buttons */}
        <TouchableOpacity style={styles.buttonGreen} 
        // onPress={stopDangerMode}
        >
          <Text style={styles.buttonText}>Run Check</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.buttonRed} 
        // onPress={handleEngineToggle}
        >
          <Text style={styles.buttonText}>
            {isEngineStopped ? "Start Engine" : "Stop Engine"}
          </Text>
        </TouchableOpacity>
      </ScrollView>

      {/* Ripple Animation when Danger Mode is Active */}
      {dangerMode && (
        <>
          {/* Create a fluid ripple effect around the screen */}
          <Animated.View
            style={[
              styles.rippleEffect,
              {
                transform: [
                  {
                    scale: waveAnim.interpolate({
                      inputRange: [0, 1],
                      outputRange: [1, 2], // Expanding ripple effect
                    }),
                  },
                ],
              },
            ]}
          >
            {/* <LinearGradient
              colors={["rgba(239, 36, 32, 0.6)", "transparent"]}
              start={{ x: 0.5, y: 0.5 }}
              end={{ x: 0.5, y: 1 }}
              style={styles.gradient}
            /> */}
          </Animated.View>

          {/* Second Ripple for Layering */}
          <Animated.View
            style={[
              styles.rippleEffect,
              {
                transform: [
                  {
                    scale: waveAnim.interpolate({
                      inputRange: [0, 1],
                      outputRange: [0.8, 1.8], // Slightly smaller ripple
                    }),
                  },
                ],
              },
            ]}
          >
            {/* <LinearGradient
              colors={["rgba(239, 36, 32, 0.4)", "transparent"]}
              start={{ x: 0.5, y: 0 }}
              end={{ x: 0.5, y: 1 }}
              style={styles.gradient}
            /> */}
          </Animated.View>
        </>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#496F76",
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#496F76",
    // marginTop: 20,
  },
  headerTitle: {
    color: "white",
    fontSize: 20,
    fontFamily: "KodchasanLight",
  },
  content: {
    padding: 20,
  },
  section: {
    marginBottom: 20,
  },
  sectionTitle: {
    color: "white",
    fontSize: 18,
    marginBottom: 10,
    fontFamily: "JuliusSansOneRegular",
  },
  sectionContent: {
    color: "white",
    fontSize: 16,
    marginBottom: 5,
    fontFamily: "KodchasanLight",
  },
  buttonGreen: {
    backgroundColor: "#44B556",
    padding: 15,
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 5,
    marginBottom: 10,
  },
  buttonRed: {
    backgroundColor: "#EF2420",
    borderRadius: 5,
    padding: 12,
    alignItems: "center",
    width: "100%",
    marginTop: 10,
  },
  buttonText: {
    color: "white",
    fontSize: 18,
    fontFamily: "JuliusSansOneRegular",
  },
  rippleEffect: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    borderRadius: 9999, // Circular ripple effect
    zIndex: -1, // Send it behind the content
  },
  gradient: {
    flex: 1,
    borderRadius: 9999,
    opacity: 0.6,
  },
});

export default BlackboxScreen;
