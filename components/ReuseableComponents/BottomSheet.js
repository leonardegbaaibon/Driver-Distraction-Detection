import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  SafeAreaView,
  Image,
  Modal,
} from "react-native";
import { BottomSheet } from "@rneui/themed";
import { Icon } from "@rneui/base";
import LottieView from "lottie-react-native";
import { BlurView } from "@react-native-community/blur";
import { useNavigation } from "@react-navigation/native";
// import { BlurView } from "expo-blur";

// type BottomSheetComponentProps = {};
const BottomSheetComponent = ({ isVisible, setIsVisible }) => {
  const navigation = useNavigation();
  const list = [
    { title: "List Item 1" },
    { title: "List Item 2" },
    {
      title: "Cancel",
      containerStyle: { backgroundColor: "red" },
      titleStyle: { color: "white" },
      onPress: () => setIsVisible(false), // Fix the onPress handler
    },
  ];

  const renderLottieAnimation = () => {
    try {
      return (
        <Image
          source={require("../../assets/vehicle.gif")}
          style={{ width: 100, height: 100 }}
        />
      );
    } catch (error) {
      console.error("Error loading Lottie animation:", error);
      return null; // or some fallback component
    }
  };

  const [trackingChecked, setTrackingChecked] = React.useState(true);
  const [blackboxChecked, setBlackboxChecked] = React.useState(false);

  return (
    <>
      {isVisible && (
        <BlurView
          intensity={3} // You can adjust the intensity of the blur
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            height:'100%',
          }}
        ></BlurView>
      )}
    <SafeAreaView style={{ flex: 1 }}>
      {/* Overlay for blur effect */}

      <BottomSheet
        isVisible={isVisible}
        containerStyle={{ backgroundColor: "rgba(0, 0, 0, 0.7)" }} // Semi-transparent background
        onBackdropPress={() => setIsVisible(false)}
      >
        <View style={styles.bottomSheetContent}>
          {renderLottieAnimation()}

          <Text style={styles.motionText}>The vehicle is in motion</Text>
          <View style={styles.checkboxContainer}>
            <TouchableOpacity
              style={styles.checkbox}
              onPress={() => setTrackingChecked(!trackingChecked)}
            >
              <Icon
                name={trackingChecked ? "check-square" : "square"}
                type="font-awesome-5"
                color={trackingChecked ? "#17C05B" : "white"}
                size={20}
              />
              <Text style={styles.checkboxLabel}>Tracking</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.checkbox}
              onPress={() => {
                navigation.navigate("BlackboxScreen");
                setIsVisible(false);
              }}
            >
              <Text style={styles.checkboxLabel}>Blackbox</Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity
            style={styles.button}
            onPress={() => setIsVisible(false)}
          >
            <Text style={styles.buttonText}>Report emergency</Text>
          </TouchableOpacity>

          <Text style={styles.integratedText}>Blackbox Integrated</Text>
        </View>
      </BottomSheet>
    </SafeAreaView>
    </>
  );
};

const styles = StyleSheet.create({
  bottomSheetContent: {
    backgroundColor: "#496F76",
    padding: 16,
    alignItems: "center",
  },
  motionText: {
    color: "white",
    fontSize: 18,
    marginBottom: 20,
    fontFamily: "JuliusSansOneRegular",
  },
  checkboxContainer: {
    flexDirection: "row",
    justifyContent: "space-around",
    width: "100%",
    marginBottom: 20,
  },
  absolute: {
    position: "absolute",
    top: 0,
    left: 0,
    bottom: 0,
    right: 0,
  },
  checkbox: {
    padding: 15,
    paddingHorizontal: 20,
    flexDirection: "row",
    marginBottom: 10,
    paddingVertical: 12,
    borderWidth: 2,
    borderRadius: 10,
    borderColor: '#333333',
    backgroundColor: '#333333',
    alignItems: "center",
  },
  checkboxLabel: {
    color:'#EEE8E0',
    marginLeft: 10,
    fontSize: 16,
    fontFamily: "JuliusSansOneRegular",
  },
  button: {
    backgroundColor: "#EF2420",
    borderRadius: 5,
    padding: 12,
    alignItems: 'center',
    width: '100%', 
    // marginBottom: 30,
    marginTop:10
  },
  buttonText: {
    color: "white",
    fontSize: 18,
    fontFamily: "JuliusSansOneRegular",
  },
  integratedText: {
    color: "white",
    fontSize: 14,
    marginTop: 20,
    opacity: 0.7,
  },
  // ... other styles you might need
});

export default BottomSheetComponent;
