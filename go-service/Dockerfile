FROM golang:1.25-alpine  
WORKDIR /go/src/app

RUN go install github.com/cosmtrek/air@v1.49.0

COPY go.mod go.sum ./
RUN go mod download

COPY . .

EXPOSE 50051

CMD ["air", "-c", ".air.toml"]
