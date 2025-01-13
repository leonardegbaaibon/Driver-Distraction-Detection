import React, { useRef } from "react";
import {
  View,
  Text,
  ScrollView,
  Dimensions,
  Animated,
  TouchableOpacity,
  Image,
} from "react-native";
import { useNavigation } from "@react-navigation/native"; // Import useNavigation

const { width } = Dimensions.get("window");

const OnboardingScreen = () => {
  const scrollX = useRef(new Animated.Value(0)).current;
  const navigation = useNavigation(); // Initialize useNavigation

  const backgroundColor = scrollX.interpolate({
    inputRange: [0, width, 2 * width],
    outputRange: ["#EEE8E0", "#E9B962", "#496F76"], // Shades of #496F76
    extrapolate: "clamp",
  });

  const screens = [
    {
      image: require("../assets/Faults.png"),
      text: "Faults Monitor",
      description: "Keep track of faults in real-time.",
    },
    {
      image: require("../assets/Track.png"),
      text: "Trips Data",
      description: "Analyze your driving trips easily.",
    },
    {
      image: require("../assets/Scorecard.png"),
      text: "Driver Badge",
      description: "Earn badges for safe driving.",
    },
  ];

  const scrollViewRef = useRef(null);

  const handleContinue = () => {
    const currentIndex = Math.floor(scrollX._value / width);
    if (currentIndex === screens.length - 1) {
      // Navigate to Login screen if on the last screen
      navigation.replace("Login");
    } else {
      // Otherwise, continue to the next screen
      scrollViewRef.current.scrollTo({
        x: scrollX._value + width,
        animated: true,
      });
    }
  };

  const handleSkip = () => {
    navigation.replace("Login"); // Navigate to Login screen when skipping
  };

  const skipButtonStyles = {
    backgroundColor: scrollX.interpolate({
      inputRange: [0, width, 2 * width],
      outputRange: ["#FFFFFF", "#FFFFFF", "transparent"], // Transparent background on the last screen
      extrapolate: "clamp",
    }),
    borderColor: scrollX.interpolate({
      inputRange: [0, width, 2 * width],
      outputRange: ["#333333", "#333333", "#FFFFFF"], // Adjust border color based on screen background
      extrapolate: "clamp",
    }),
    color: scrollX.interpolate({
      inputRange: [0, width, 2 * width],
      outputRange: ["#333333", "#333333", "#FFFFFF"], // Adjust text color
      extrapolate: "clamp",
    }),
  };

  return (
    <Animated.View style={{ flex: 1, backgroundColor }}>
      <View
        style={{
          position: "absolute",
          top: 50,
          width: "100%",
          alignItems: "center",
        }}
      >
        <View
          style={{
            flexDirection: "row",
            justifyContent: "center",
            alignItems: "center",
          }}
        >
          {/* Pagination Dots */}
          {screens.map((_, i) => {
            const scale = scrollX.interpolate({
              inputRange: [(i - 1) * width, i * width, (i + 1) * width],
              outputRange: [1, 1.5, 1], // Scale effect to highlight active dot
              extrapolate: "clamp",
            });

            return (
              <Animated.View
                key={i}
                style={{
                  width: 30, // Adjust length of dots for better visibility
                  height: 6,
                  backgroundColor: scrollX.interpolate({
                    inputRange: [(i - 1) * width, i * width, (i + 1) * width],
                    outputRange: ["#C1D6D9", "#FFFFFF", "#C1D6D9"], // White for active dot, lighter shades for inactive
                    extrapolate: "clamp",
                  }),
                  marginHorizontal: 4,
                  borderRadius: 3,
                  transform: [{ scale }],
                }}
              />
            );
          })}
        </View>
      </View>

      <ScrollView
        ref={scrollViewRef}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onScroll={Animated.event(
          [{ nativeEvent: { contentOffset: { x: scrollX } } }],
          { useNativeDriver: false }
        )}
        scrollEventThrottle={16}
      >
        {screens.map((screen, index) => (
          <View
            key={index}
            style={{
              width,
              justifyContent: "center",
              alignItems: "center",
              paddingHorizontal: 20,
            }}
          >
            <Animated.View
              style={{
                width: 200,
                height: 200,
                borderRadius: 20,
                justifyContent: "center",
                alignItems: "center",
                transform: [
                  {
                    scale: scrollX.interpolate({
                      inputRange: [
                        (index - 1) * width,
                        index * width,
                        (index + 1) * width,
                      ],
                      outputRange: [0.8, 1.2, 0.8],
                      extrapolate: "clamp",
                    }),
                  },
                ],
              }}
            >
              <Image
                source={screen.image}
                style={{ width: 120, height: 120, marginBottom: 20 }}
              />
              <Text
                style={{
                  fontSize: 22,
                  fontFamily: "JuliusSansOneRegular",
                }}
              >
                {screen.text}
              </Text>
              <Text
                style={{
                  marginTop: 10,
                  textAlign: "center",
                  paddingHorizontal: 20,
                  fontFamily: "JuliusSansOneRegular",
                }}
              >
                {screen.description}
              </Text>
            </Animated.View>
          </View>
        ))}
      </ScrollView>

      {/* Buttons */}
      <View style={{ padding: 20 }}>
        <TouchableOpacity
          onPress={handleSkip}
          style={{
            marginBottom: 10,
            paddingVertical: 12,
            borderWidth: 2,
            borderRadius: 10,
            borderColor: skipButtonStyles.borderColor,
            backgroundColor: skipButtonStyles.backgroundColor,
            alignItems: "center",
          }}
        >
          <Animated.Text
            style={{
              fontSize: 18,
              color: skipButtonStyles.color,
              fontFamily: "JuliusSansOneRegular",
            }}
          >
            Skip
          </Animated.Text>
        </TouchableOpacity>
        <TouchableOpacity
          onPress={handleContinue}
          style={{
            backgroundColor: "#333333",
            paddingVertical: 12,
            borderRadius: 10,
            alignItems: "center",
          }}
        >
          <Text
            style={{
              fontSize: 18,
              color: "#FFFFFF",
              fontFamily: "JuliusSansOneRegular",
            }}
          >
            Continue
          </Text>
        </TouchableOpacity>
      </View>
    </Animated.View>
  );
};

export default OnboardingScreen;
