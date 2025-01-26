import React from "react";
import { TouchableOpacity, View, Text, StyleSheet } from "react-native";
// import { Ionicons } from "react-native-vector-icons";
import { Image } from "react-native";

const TripEntry = ({ startLocation, endLocation, duration }) => {
  return (
    <View style={styles.container}>
      <View style={styles.locationContainer}>
        {/* <Ionicons name="location" size={24} color="#FE3221" /> */}
        <View style={styles.line} />
        {/* <Ionicons name="location-outline" size={24} color="#21FE3E" /> */}
      </View>
      <View style={styles.details}>
        <Text style={styles.location}>{startLocation}</Text>
        {duration && <Text style={styles.duration}>{duration}</Text>}
        <Text style={styles.location}>{endLocation}</Text>
      </View>
      <View>
        <Image
          source={require("../../assets/TripImage.png")}
          style={{ width: 100, height: 75, marginBottom: 5 }}
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(237, 213, 173, 0.27)",
    padding: 10,
    marginVertical: 8,
    borderRadius: 10,
  },
  locationContainer: {
    alignItems: "center",
    justifyContent: "center", // Add this to ensure the icons are centered with the line
    marginRight: 16,
    height: 50, // Set a fixed height for the container
  },
  line: {
    width: 1,
    height: 22, // Set a height for the line
    backgroundColor: "white",
    marginVertical: 0,
  },
  details: {
    flex: 1, // Take up remaining space
  },
  location: {
    color: "white",
    fontSize: 14,
    fontFamily: "JuliusSansOneRegular",
  },
  duration: {
    color: "#B8B5C3",
    fontSize: 12,
    marginVertical: 4,
    fontFamily: "KodchasanLight",
  },
});

export default TripEntry;
