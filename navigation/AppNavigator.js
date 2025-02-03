import React, { useEffect, useState } from "react";
import { NavigationContainer } from "@react-navigation/native";
import { createStackNavigator } from "@react-navigation/stack";
import LoginScreen from "../screens/LoginScreen";
// import Dashboard from "../screens/Dashboard";
import AsyncStorage from "@react-native-async-storage/async-storage";
import AccountOption from "../screens/AccountOption";
// import BlackboxNoCoverageScreen from "../screens/Onboarding/NoCoverage/Blackbox/BlackboxUser";
// import CarDetails from "../screens/Onboarding/CarDetails";
// import SmartPhoneSignUp from "../screens/Onboarding/NoCoverage/Smartphone/SmartPhoneSignUp";
import MyTabs from "./BottomTabNavigation/Tabs";
import SignUpScreen from "../screens/SignUpScreen";
// import CustomSplashScreen from "../screens/CustomSplashScreen";
// import FaultDetailsScreen from "../screens/FaultsDetailsScreen";
// import TripsDetailsScreen from "../screens/TripsDetailsScreen";
// import BlackboxCoverageScreen from "../screens/Onboarding/Coverage/Blackbox/BlackboxUserCoverage";
// import ScorecardScreen from "../screens/ScorecardScreen";
// import LeaderboardScreen from "../screens/LeaderboardScreen";

const Stack = createStackNavigator();

const AppNavigator = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    AsyncStorage.setItem("token", "");
  });

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerShown: false, // Hide the header for screens in this stack navigator
        }}
      >
        {/* {isAuthenticated ? (
          ) : (
            )} */}
        {/* <Stack.Screen name="splashScreen" component={CustomSplashScreen} /> */}
        <Stack.Screen name="Accountoption" component={AccountOption} />
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="SignUp" component={SignUpScreen} />
        {/* <Stack.Screen name="faultsDetailsScreen" component={FaultDetailsScreen} />
        <Stack.Screen name="TripDetailsScreen" component={TripsDetailsScreen} />
        <Stack.Screen name="smartphonesignup" component={SmartPhoneSignUp} />
        <Stack.Screen name="cardetails" component={CarDetails} />
        <Stack.Screen name="ScorecardScreen" component={ScorecardScreen} />
        <Stack.Screen name="LeaderboardScreen" component={LeaderboardScreen} />*/}
        <Stack.Screen name="myTabs" component={MyTabs} /> 
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default AppNavigator;
