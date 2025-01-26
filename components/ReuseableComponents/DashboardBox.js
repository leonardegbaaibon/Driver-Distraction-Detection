import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated } from 'react-native';

const DashboardBox = ({ title, icon, onPress }) => {
  const bounceAnim = useRef(new Animated.Value(1)).current; // Animated value for scaling

  useEffect(() => {
    const startBouncing = () => {
      Animated.loop(
        Animated.sequence([
          Animated.timing(bounceAnim, {
            toValue: 1.1, // Scale up to 1.1 times
            duration: 300, // Duration of bounce
            useNativeDriver: true,
          }),
          Animated.timing(bounceAnim, {
            toValue: 1, // Scale back to original size
            duration: 300,
            useNativeDriver: true,
          }),
        ])
      ).start();
    };

    startBouncing(); // Start the bounce animation
  }, [bounceAnim]);

  return (
    <TouchableOpacity style={styles.boxContainer} onPress={onPress}>
      <Text style={styles.title}>{title}</Text>
      <Animated.View style={[styles.iconContainer, { transform: [{ scale: bounceAnim }] }]}>
        {icon}
      </Animated.View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  boxContainer: {
    backgroundColor: 'rgba(237, 213, 173, 0.20)', // Dark background color for the box
    borderRadius: 10, // Rounded corners
    padding: 20, // Padding inside the box
    alignItems: 'center', // Center items horizontally
    justifyContent: 'space-between', // Center items vertically
    // elevation: 3, // Shadow for Android
    shadowColor: '#000', // Shadow for iOS
    shadowOffset: { width: 0, height: 2 }, // Shadow offset for iOS
    shadowOpacity: 0.3, // Shadow opacity for iOS
    shadowRadius: 4, // Shadow radius for iOS
    margin: 10, // Margin around the box
  },
  iconContainer: {
    marginBottom: 10, // Space between icon and text
  },
  title: {
    color: 'white', // Text color
    fontSize: 18, // Text size
    fontFamily: "KodchasanRegular"
  },
});

export default DashboardBox;
