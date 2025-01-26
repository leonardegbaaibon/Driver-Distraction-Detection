import ApiManager from "../ApiManager/Apimanager";

export const SignUpApi = (data) => {
  const result = ApiManager("/v2/gumshoe/sign.up", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      'accept': 'application/json'
        },
    data:data,
  });
  return result;
};
