import React, { useState, useEffect, useRef } from "react";
import { View, Text, StyleSheet } from "react-native";
import {
  Svg,
  Path,
  Defs,
  LinearGradient,
  Stop,
  Text as SvgText,
  Rect,
} from "react-native-svg";
import SpeedIcon from "../../assets/SpeedIcon";

const size = 240; // Adjust size to fit your layout
const Speedometer = ({ maxValue = 240 }) => {
  const [value, setValue] = useState(0); // Current displayed value
  const valueRef = useRef(0); // Ref to track the actual value (used for animation)
  const strokeWidth = 12;
  const textFontSize = 20; // Font size for the value text
  const textWidth = 120; // Approx width of the text background box
  const textHeight = 120; // Height of the text background box
  const radius = (size - strokeWidth * 2) / 2;
  const circumference = radius * Math.PI; // Semi-circle circumference
  const halfCircle = size / 2;
  const shadowOpacity = 0.5; // Adjust as needed
  const shadowOffset = 1; // Adjust as needed
  const shadowCount = 7; // Number of shadow layers

  const animationDuration = 1000; // 1 second for animation

  const renderShadowRects = () => {
    const shadows = [];
    for (let i = 0; i < shadowCount; i++) {
      shadows.push(
        <Rect
          key={`shadow-${i}`}
          x={halfCircle - textWidth / 2 - i * shadowOffset}
          y={halfCircle - textHeight / 2 - i * shadowOffset}
          rx={textHeight / 2}
          ry={textHeight / 2}
          width={textWidth + i * shadowOffset * 2}
          height={textHeight + i * shadowOffset * 2}
          fill={`rgba(221, 221, 221, ${
            (shadowOpacity / shadowCount) * (shadowCount - i)
          })`}
        />
      );
    }
    return shadows;
  };

  // Function to convert polar coordinates to cartesian
  function polarToCartesian(centerX, centerY, radius, angleInDegrees) {
    var angleInRadians = ((angleInDegrees - 180) * Math.PI) / 180.0;

    return {
      x: centerX + radius * Math.cos(angleInRadians),
      y: centerY + radius * Math.sin(angleInRadians),
    };
  }

  // Function to describe an arc using SVG's path commands
  function describeArc(x, y, radius, startAngle, endAngle) {
    var start = polarToCartesian(x, y, radius, endAngle);
    var end = polarToCartesian(x, y, radius, startAngle);

    var largeArcFlag = endAngle - startAngle <= 180 ? "0" : "1";

    var d = [
      "M",
      start.x,
      start.y,
      "A",
      radius,
      radius,
      0,
      largeArcFlag,
      0,
      end.x,
      end.y,
    ].join(" ");

    return d;
  }

  // Animate the speedometer value gradually to the target value
  const animateValue = (startValue, endValue, duration) => {
    const startTime = performance.now();

    const animateStep = (currentTime) => {
      const elapsedTime = currentTime - startTime;
      const progress = Math.min(elapsedTime / duration, 1); // Ensure progress is between 0 and 1
      const currentValue = startValue + (endValue - startValue) * progress;

      // Update the displayed value
      setValue(currentValue);

      // Continue the animation if it's not done
      if (progress < 1) {
        requestAnimationFrame(animateStep);
      } else {
        valueRef.current = endValue; // Update the ref when done
      }
    };

    requestAnimationFrame(animateStep);
  };

  // Update the speedometer value every 2 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      // Generate a random value between 20 and 240
      const randomValue = Math.floor(Math.random() * (240 - 20 + 1)) + 20;

      // Animate from the current value to the new random value
      animateValue(valueRef.current, randomValue, animationDuration);
    }, 2000); // Change value every 2 seconds

    return () => clearInterval(interval); // Cleanup on unmount
  }, []);

  const getGradientColor = (speed) => {
    if (speed > 100) {
      return "red";
    }
    return "#00e676";
  };

  // Calculate the stroke dash offset for the value
  const strokeDashoffset = circumference - (value / maxValue) * circumference;

  return (
    <View style={styles.container}>
      <Svg height={size} width={size} viewBox={`0 0 ${size} ${size}`}>
        <Defs>
          <LinearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="0%">
            <Stop offset="0%" stopColor={getGradientColor(value)} />
            <Stop offset="1" stopColor={getGradientColor(value)} />
          </LinearGradient>
        </Defs>
        {/* Background Arc */}
        <Path
          d={describeArc(halfCircle, halfCircle, radius, 0, 180)}
          fill="none"
          stroke="rgba(0, 0, 0, 0.4)"
          strokeWidth={strokeWidth}
          strokeLinecap="round"
        />
        {/* Foreground Arc with smooth animation */}
        <Path
          d={describeArc(halfCircle, halfCircle, radius, 0, (value / maxValue) * 180)}
          fill="none"
          stroke="url(#grad)"
          strokeWidth={strokeWidth}
          strokeDashoffset={strokeDashoffset}
          strokeLinecap="round"
        />
        {renderShadowRects()}
        {/* Rounded box for the value */}
        <Rect
          x={halfCircle - textWidth / 2}
          y={halfCircle - textHeight / 2}
          rx={textHeight / 2}
          ry={textHeight / 2}
          width={textWidth}
          height={textHeight}
          fill="white"
        />
        {/* Value Text */}
        <SvgText
          x={halfCircle}
          y={halfCircle + textFontSize / 2} 
          textAnchor="middle"
          fill="#0F084B"
          fontSize={textFontSize}
          fontFamily="JuliusSansOneRegular" // Ensure correct fontFamily usage
        >
          {`${Math.round(value)} km/h`} {/* Rounded the value */}
        </SvgText>
      </Svg>
      <View style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
        <SpeedIcon />
        <Text style={styles.speedometerText}>Speed Meter</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: "center",
    justifyContent: "center",
    padding: 20,
    borderRadius: size / 2,

  },
  speedometerText: {
    fontSize: 18,
    color: "white",
    marginHorizontal: 10,
    fontFamily: "KodchasanRegular",
  },
});

export default Speedometer;
