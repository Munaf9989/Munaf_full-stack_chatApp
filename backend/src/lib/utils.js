import jwt from "jsonwebtoken";

export const generateToken = (userId, res) => {

  const token = jwt.sign(
    { userId },
    process.env.JWT_SECRET,
    {
      expiresIn: "7d",
    }
  );

  const isProduction = process.env.NODE_ENV === "production";

  res.cookie("jwt", token, {
    maxAge: 7 * 24 * 60 * 60 * 1000,
    httpOnly: true,
    // In production (EKS behind ingress), cookies must be sameSite=none + secure=true
    // so the browser sends them on cross-origin requests routed through the ingress.
    sameSite: isProduction ? "none" : "lax",
    secure: isProduction,
  });

  return token;
};
