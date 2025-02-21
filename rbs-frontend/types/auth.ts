export interface IUser {
    email: string
    role: 'STUDENT' | 'ADMINISTRATOR'
    exp?: number // JWT expiration timestamp
  }
  
  export interface IAuthTokenPayload {
    sub: string   // email
    role: IUser['role']
    iat: number   // issued at timestamp
    exp: number   // expiration timestamp
  }