import React, { useEffect, useState } from 'react';
import { Modal, View, ActivityIndicator, StyleSheet } from 'react-native';

const LoadingModal = ({ isVisible }) => {
  const [modalVisible, setModalVisible] = useState(false);

  useEffect(() => {
    if (isVisible) {
      setModalVisible(true);
      // You can add any additional logic here to trigger the animation
    } else {
      setModalVisible(false);
    }
  }, [isVisible]);

  return (
    <Modal
      animationType="slide"
      transparent={true}
      visible={modalVisible}
      onRequestClose={() => setModalVisible(false)}
    >
      <View style={styles.modalContainer}>
        <ActivityIndicator
          size="large" // You can adjust the size as needed
          color="#ffffff" // Set the color of the spinner
          style={styles.modalSpinner}
        />
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  modalSpinner: {
    width: 100,
    height: 100,
  },
});

export default LoadingModal;
