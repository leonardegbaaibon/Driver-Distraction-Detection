import axios from "axios";

const ApiManager = axios.create({
  baseURL: "https://api.blackboxservice.monster",
  // responseType:"json",
  // withCredentials:"true"
});

export default ApiManager;
